package shop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Armazenamento em PostgreSQL. Duas instâncias podem usar o mesmo banco: a consistência vem de
 * transações e travas de linha (SELECT ... FOR UPDATE), nunca de memória do processo.
 */
class JdbcStore implements Store, AutoCloseable {
    private static final long SCHEMA_LOCK_ID = 7_351_001L;

    private static final List<String> SCHEMA = List.of(
            """
            CREATE TABLE IF NOT EXISTS products (
              sku text PRIMARY KEY,
              name text NOT NULL,
              price numeric(14,2) NOT NULL,
              weight_kg numeric(10,3) NOT NULL,
              stock integer NOT NULL CHECK (stock >= 0),
              discontinued boolean NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS customers (
              cpf text PRIMARY KEY,
              email text NOT NULL,
              credit_balance numeric(14,2) NOT NULL DEFAULT 0
            )""",
            """
            CREATE TABLE IF NOT EXISTS orders (
              id text PRIMARY KEY,
              cpf text NOT NULL,
              email text NOT NULL,
              status text NOT NULL,
              created_at timestamptz NOT NULL,
              summary text NOT NULL,
              tracking_code text,
              paid_at timestamptz,
              transaction_id text
            )""",
            "CREATE INDEX IF NOT EXISTS orders_cpf_status_idx ON orders (cpf, status)",
            """
            CREATE TABLE IF NOT EXISTS order_items (
              order_id text NOT NULL REFERENCES orders (id),
              position integer NOT NULL,
              sku text NOT NULL,
              quantity integer NOT NULL,
              PRIMARY KEY (order_id, position)
            )""",
            """
            CREATE TABLE IF NOT EXISTS payment_events (
              order_id text NOT NULL,
              transaction_id text NOT NULL,
              error_code text,
              http_status integer NOT NULL,
              PRIMARY KEY (order_id, transaction_id)
            )""",
            """
            CREATE TABLE IF NOT EXISTS notifications (
              id bigserial PRIMARY KEY,
              cpf text NOT NULL,
              type text NOT NULL,
              order_id text NOT NULL,
              status text NOT NULL
            )""",
            "CREATE INDEX IF NOT EXISTS notifications_cpf_idx ON notifications (cpf, id)");

    private final HikariDataSource dataSource;

    JdbcStore(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000);
        this.dataSource = new HikariDataSource(config);
        initSchema();
    }

    /** Cria tabelas e catálogo; seguro com duas instâncias subindo ao mesmo tempo. */
    private void initSchema() {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(true);
            try (Statement st = c.createStatement()) {
                st.execute("SELECT pg_advisory_lock(" + SCHEMA_LOCK_ID + ")");
            }
            try {
                try (Statement st = c.createStatement()) {
                    for (String ddl : SCHEMA) st.execute(ddl);
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) VALUES (?,?,?,?,?,?) "
                                + "ON CONFLICT (sku) DO NOTHING")) {
                    for (Product p : Catalog.initial()) {
                        ps.setString(1, p.sku());
                        ps.setString(2, p.name());
                        ps.setBigDecimal(3, p.price());
                        ps.setBigDecimal(4, p.weightKg());
                        ps.setInt(5, p.stock());
                        ps.setBoolean(6, p.discontinued());
                        ps.executeUpdate();
                    }
                }
            } finally {
                try (Statement st = c.createStatement()) {
                    st.execute("SELECT pg_advisory_unlock(" + SCHEMA_LOCK_ID + ")");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize database", e);
        }
    }

    @Override
    public <T> T transaction(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new JdbcTx(c));
                c.commit();
                return result;
            } catch (Throwable t) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                    // a falha original é a que importa
                }
                throw t;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static OffsetDateTime ts(Instant i) {
        return i == null ? null : OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private static final class JdbcTx implements Tx {
        private final Connection c;

        JdbcTx(Connection c) {
            this.c = c;
        }

        private <T> T sql(SqlWork<T> work) {
            try {
                return work.run();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        private static Product product(ResultSet rs) throws SQLException {
            return new Product(rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price"),
                    rs.getBigDecimal("weight_kg"), rs.getInt("stock"), rs.getBoolean("discontinued"));
        }

        public Optional<Product> findProduct(String sku) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE sku = ?")) {
                    ps.setString(1, sku);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(product(rs)) : Optional.empty();
                    }
                }
            });
        }

        public List<Product> lockProducts(Collection<String> skus) {
            return sql(() -> {
                List<Product> result = new ArrayList<>();
                for (String sku : new TreeSet<>(skus)) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE sku = ? FOR UPDATE")) {
                        ps.setString(1, sku);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) result.add(product(rs));
                        }
                    }
                }
                return result;
            });
        }

        public void addStock(String sku, int delta) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = stock + ? WHERE sku = ?")) {
                    ps.setInt(1, delta);
                    ps.setString(2, sku);
                    ps.executeUpdate();
                }
                return null;
            });
        }

        private Optional<Order> loadOrder(String id, boolean lock) {
            return sql(() -> {
                Order base;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM orders WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return Optional.<Order>empty();
                        base = new Order(rs.getString("id"), rs.getString("cpf"), rs.getString("email"),
                                OrderStatus.valueOf(rs.getString("status")),
                                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                                Json.read(rs.getString("summary"), Summary.class), null,
                                rs.getString("tracking_code"),
                                Optional.ofNullable(rs.getObject("paid_at", OffsetDateTime.class))
                                        .map(OffsetDateTime::toInstant).orElse(null),
                                rs.getString("transaction_id"));
                    }
                }
                List<OrderItem> items = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT sku, quantity FROM order_items WHERE order_id = ? ORDER BY position")) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) items.add(new OrderItem(rs.getString(1), rs.getInt(2)));
                    }
                }
                return Optional.of(new Order(base.id(), base.cpf(), base.email(), base.status(), base.createdAt(),
                        base.summary(), items, base.trackingCode(), base.paidAt(), base.transactionId()));
            });
        }

        public Optional<Order> findOrder(String id) {
            return loadOrder(id, false);
        }

        public Optional<Order> lockOrder(String id) {
            return loadOrder(id, true);
        }

        public void insertOrder(Order o) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO orders (id, cpf, email, status, created_at, summary, tracking_code, paid_at, "
                                + "transaction_id) VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, o.id());
                    ps.setString(2, o.cpf());
                    ps.setString(3, o.email());
                    ps.setString(4, o.status().name());
                    ps.setObject(5, ts(o.createdAt()));
                    ps.setString(6, Json.write(o.summary()));
                    ps.setString(7, o.trackingCode());
                    ps.setObject(8, ts(o.paidAt()));
                    ps.setString(9, o.transactionId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_items (order_id, position, sku, quantity) VALUES (?,?,?,?)")) {
                    int position = 0;
                    for (OrderItem item : o.items()) {
                        ps.setString(1, o.id());
                        ps.setInt(2, position++);
                        ps.setString(3, item.sku());
                        ps.setInt(4, item.quantity());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                return null;
            });
        }

        public void updateOrder(Order o) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE orders SET status = ?, tracking_code = ?, paid_at = ?, transaction_id = ? WHERE id = ?")) {
                    ps.setString(1, o.status().name());
                    ps.setString(2, o.trackingCode());
                    ps.setObject(3, ts(o.paidAt()));
                    ps.setString(4, o.transactionId());
                    ps.setString(5, o.id());
                    ps.executeUpdate();
                }
                return null;
            });
        }

        public boolean hasFulfilledOrder(String cpf) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM orders WHERE cpf = ? AND status IN ('PAID','SHIPPED','DELIVERED') LIMIT 1")) {
                    ps.setString(1, cpf);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            });
        }

        public void ensureCustomer(String cpf, String email) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO customers (cpf, email, credit_balance) VALUES (?, ?, 0) ON CONFLICT (cpf) DO NOTHING")) {
                    ps.setString(1, cpf);
                    ps.setString(2, email);
                    ps.executeUpdate();
                }
                return null;
            });
        }

        public Optional<Customer> findCustomer(String cpf) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT credit_balance FROM customers WHERE cpf = ?")) {
                    ps.setString(1, cpf);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(new Customer(cpf, rs.getBigDecimal(1))) : Optional.empty();
                    }
                }
            });
        }

        public void addCredit(String cpf, BigDecimal delta) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?")) {
                    ps.setBigDecimal(1, delta);
                    ps.setString(2, cpf);
                    ps.executeUpdate();
                }
                return null;
            });
        }

        public Optional<PaymentRecord> findPayment(String orderId, String transactionId) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT error_code, http_status FROM payment_events WHERE order_id = ? AND transaction_id = ?")) {
                    ps.setString(1, orderId);
                    ps.setString(2, transactionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next()
                                ? Optional.of(new PaymentRecord(rs.getString(1), rs.getInt(2)))
                                : Optional.empty();
                    }
                }
            });
        }

        public void savePayment(String orderId, String transactionId, PaymentRecord record) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO payment_events (order_id, transaction_id, error_code, http_status) VALUES (?,?,?,?) "
                                + "ON CONFLICT (order_id, transaction_id) DO NOTHING")) {
                    ps.setString(1, orderId);
                    ps.setString(2, transactionId);
                    ps.setString(3, record.errorCode());
                    ps.setInt(4, record.httpStatus());
                    ps.executeUpdate();
                }
                return null;
            });
        }

        private static Notification notification(ResultSet rs) throws SQLException {
            return new Notification(rs.getLong("id"), rs.getString("cpf"), NotificationType.valueOf(rs.getString("type")),
                    rs.getString("order_id"), NotificationStatus.valueOf(rs.getString("status")));
        }

        public Notification addNotification(String cpf, NotificationType type, String orderId) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO notifications (cpf, type, order_id, status) VALUES (?,?,?,'NEW') RETURNING *")) {
                    ps.setString(1, cpf);
                    ps.setString(2, type.name());
                    ps.setString(3, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        return notification(rs);
                    }
                }
            });
        }

        public List<Notification> notifications(String cpf) {
            return sql(() -> {
                List<Notification> result = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM notifications WHERE cpf = ? ORDER BY id")) {
                    ps.setString(1, cpf);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) result.add(notification(rs));
                    }
                }
                return result;
            });
        }

        public Optional<Notification> markRead(long id) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE notifications SET status = 'READ' WHERE id = ? RETURNING *")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(notification(rs)) : Optional.empty();
                    }
                }
            });
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run() throws SQLException;
    }
}
