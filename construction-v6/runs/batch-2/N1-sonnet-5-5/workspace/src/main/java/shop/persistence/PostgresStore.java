package shop.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shop.catalog.Product;
import shop.catalog.ProductRepository;
import shop.checkout.Summary;
import shop.customer.Customer;
import shop.customer.CustomerRepository;
import shop.notification.Notification;
import shop.notification.NotificationRepository;
import shop.notification.NotificationStatus;
import shop.notification.NotificationType;
import shop.order.Order;
import shop.order.OrderItem;
import shop.order.OrderRepository;
import shop.order.OrderStatus;
import shop.order.PaymentNotice;
import shop.order.PaymentNoticeRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PostgresStore implements Store {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final DataSource dataSource;

    public PostgresStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T inTransaction(Function<Tx, T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(new PostgresTx(connection));
                connection.commit();
                return result;
            } catch (Throwable e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class PostgresTx implements Tx {
        private final Connection connection;

        PostgresTx(Connection connection) {
            this.connection = connection;
        }

        @Override
        public ProductRepository products() {
            return new Products(connection);
        }

        @Override
        public OrderRepository orders() {
            return new Orders(connection);
        }

        @Override
        public CustomerRepository customers() {
            return new Customers(connection);
        }

        @Override
        public NotificationRepository notifications() {
            return new Notifications(connection);
        }

        @Override
        public PaymentNoticeRepository paymentNotices() {
            return new PaymentNotices(connection);
        }
    }

    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private interface RowReader<T> {
        T read(ResultSet row) throws SQLException;
    }

    private static void update(Connection connection, String sql, Binder binder) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> List<T> query(Connection connection, String sql, Binder binder, RowReader<T> reader) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(reader.read(rows));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> Optional<T> queryOne(Connection connection, String sql, Binder binder, RowReader<T> reader) {
        return query(connection, sql, binder, reader).stream().findFirst();
    }

    private record Products(Connection connection) implements ProductRepository {
        private static final String COLUMNS = "SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = ?";

        private static Product read(ResultSet row) throws SQLException {
            return new Product(row.getString("sku"), row.getString("name"), row.getBigDecimal("price"),
                    row.getBigDecimal("weight_kg"), row.getInt("stock"), row.getBoolean("discontinued"));
        }

        @Override
        public Optional<Product> find(String sku) {
            return queryOne(connection, COLUMNS, s -> s.setString(1, sku), Products::read);
        }

        @Override
        public Optional<Product> lock(String sku) {
            return queryOne(connection, COLUMNS + " FOR UPDATE", s -> s.setString(1, sku), Products::read);
        }

        @Override
        public void adjustStock(String sku, int delta) {
            update(connection, "UPDATE products SET stock = stock + ? WHERE sku = ?", s -> {
                s.setInt(1, delta);
                s.setString(2, sku);
            });
        }

        @Override
        public void insertIfAbsent(Product product) {
            update(connection, "INSERT INTO products (sku, name, price, weight_kg, stock, discontinued) "
                    + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (sku) DO NOTHING", s -> {
                s.setString(1, product.sku());
                s.setString(2, product.name());
                s.setBigDecimal(3, product.price());
                s.setBigDecimal(4, product.weightKg());
                s.setInt(5, product.stock());
                s.setBoolean(6, product.discontinued());
            });
        }
    }

    private record Customers(Connection connection) implements CustomerRepository {
        @Override
        public void createIfAbsent(String cpf) {
            update(connection, "INSERT INTO customers (cpf, credit_balance) VALUES (?, 0) ON CONFLICT (cpf) DO NOTHING",
                    s -> s.setString(1, cpf));
        }

        @Override
        public Optional<Customer> find(String cpf) {
            return queryOne(connection, "SELECT cpf, credit_balance FROM customers WHERE cpf = ?",
                    s -> s.setString(1, cpf),
                    row -> new Customer(row.getString("cpf"), row.getBigDecimal("credit_balance")));
        }

        @Override
        public void addCredit(String cpf, BigDecimal amount) {
            update(connection, "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?", s -> {
                s.setBigDecimal(1, amount);
                s.setString(2, cpf);
            });
        }
    }

    private record Orders(Connection connection) implements OrderRepository {
        private static final String SELECT =
                "SELECT id, customer_cpf, email, status, created_at, summary, tracking_code FROM orders WHERE id = ?";

        @Override
        public void insert(Order order) {
            PostgresStore.update(connection,
                    "INSERT INTO orders (id, customer_cpf, email, status, created_at, summary, tracking_code) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)", s -> {
                        s.setString(1, order.id());
                        s.setString(2, order.customerCpf());
                        s.setString(3, order.email());
                        s.setString(4, order.status().name());
                        s.setObject(5, OffsetDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC));
                        s.setString(6, toJson(order.summary()));
                        s.setString(7, order.trackingCode());
                    });
            for (OrderItem item : order.items()) {
                PostgresStore.update(connection, "INSERT INTO order_items (order_id, sku, quantity) VALUES (?, ?, ?)", s -> {
                    s.setString(1, order.id());
                    s.setString(2, item.sku());
                    s.setInt(3, item.quantity());
                });
            }
        }

        @Override
        public Optional<Order> find(String id) {
            return queryOne(connection, SELECT, s -> s.setString(1, id), this::read);
        }

        @Override
        public Optional<Order> lock(String id) {
            return queryOne(connection, SELECT + " FOR UPDATE", s -> s.setString(1, id), this::read);
        }

        @Override
        public void update(Order order) {
            PostgresStore.update(connection, "UPDATE orders SET status = ?, tracking_code = ? WHERE id = ?", s -> {
                s.setString(1, order.status().name());
                s.setString(2, order.trackingCode());
                s.setString(3, order.id());
            });
        }

        @Override
        public boolean hasConfirmedPurchase(String cpf) {
            return queryOne(connection,
                    "SELECT 1 FROM orders WHERE customer_cpf = ? AND status IN ('PAID', 'SHIPPED', 'DELIVERED') LIMIT 1",
                    s -> s.setString(1, cpf), row -> 1).isPresent();
        }

        private Order read(ResultSet row) throws SQLException {
            String id = row.getString("id");
            List<OrderItem> items = query(connection,
                    "SELECT sku, quantity FROM order_items WHERE order_id = ? ORDER BY sku",
                    s -> s.setString(1, id), r -> new OrderItem(r.getString("sku"), r.getInt("quantity")));
            try {
                return new Order(id, row.getString("customer_cpf"), row.getString("email"),
                        OrderStatus.valueOf(row.getString("status")),
                        row.getObject("created_at", OffsetDateTime.class).toInstant(),
                        JSON.readValue(row.getString("summary"), Summary.class), items, row.getString("tracking_code"));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String toJson(Summary summary) {
            try {
                return JSON.writeValueAsString(summary);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private record Notifications(Connection connection) implements NotificationRepository {
        private static final String SELECT = "SELECT id, type, order_id, status, customer_cpf FROM notifications";

        private static Notification read(ResultSet row) throws SQLException {
            return new Notification(row.getString("id"), NotificationType.valueOf(row.getString("type")),
                    row.getString("order_id"), NotificationStatus.valueOf(row.getString("status")),
                    row.getString("customer_cpf"));
        }

        @Override
        public void insert(Notification notification) {
            update(connection, "INSERT INTO notifications (id, customer_cpf, type, order_id, status) VALUES (?, ?, ?, ?, ?)",
                    s -> {
                        s.setString(1, notification.id());
                        s.setString(2, notification.customerCpf());
                        s.setString(3, notification.type().name());
                        s.setString(4, notification.orderId());
                        s.setString(5, notification.status().name());
                    });
        }

        @Override
        public List<Notification> findByCustomer(String cpf) {
            return query(connection, SELECT + " WHERE customer_cpf = ? ORDER BY seq", s -> s.setString(1, cpf),
                    Notifications::read);
        }

        @Override
        public Optional<Notification> find(String id) {
            return queryOne(connection, SELECT + " WHERE id = ?", s -> s.setString(1, id), Notifications::read);
        }

        @Override
        public void markRead(String id) {
            update(connection, "UPDATE notifications SET status = 'READ' WHERE id = ?", s -> s.setString(1, id));
        }
    }

    private record PaymentNotices(Connection connection) implements PaymentNoticeRepository {
        @Override
        public Optional<PaymentNotice> find(String orderId, String transactionId) {
            return queryOne(connection,
                    "SELECT order_id, transaction_id, refusal FROM payment_notices WHERE order_id = ? AND transaction_id = ?",
                    s -> {
                        s.setString(1, orderId);
                        s.setString(2, transactionId);
                    },
                    row -> new PaymentNotice(row.getString("order_id"), row.getString("transaction_id"),
                            row.getString("refusal")));
        }

        @Override
        public void insert(PaymentNotice notice) {
            update(connection, "INSERT INTO payment_notices (order_id, transaction_id, refusal) VALUES (?, ?, ?)", s -> {
                s.setString(1, notice.orderId());
                s.setString(2, notice.transactionId());
                s.setString(3, notice.refusalCode());
            });
        }
    }
}
