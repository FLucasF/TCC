package shop.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import shop.domain.Customer;
import shop.domain.Json;
import shop.domain.Money;
import shop.domain.Notification;
import shop.domain.NotificationType;
import shop.domain.Order;
import shop.domain.OrderItem;
import shop.domain.OrderStatus;
import shop.domain.Product;
import shop.domain.Summary;

/** All SQL of the service, bound to one transaction. */
public final class Dao {
    private static final String PRODUCT_COLUMNS = "sku, name, price, weight_kg, stock, discontinued";

    private final Connection c;
    private final boolean postgres;

    Dao(Connection c, boolean postgres) {
        this.c = c;
        this.postgres = postgres;
    }

    // ---- products

    public Optional<Product> product(String sku) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT " + PRODUCT_COLUMNS + " FROM products WHERE sku = ?")) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(product(rs)) : Optional.empty();
            }
        }
    }

    public Map<String, Product> products(Collection<String> skus) throws SQLException {
        Map<String, Product> result = new LinkedHashMap<>();
        for (String sku : skus) {
            product(sku).ifPresent(p -> result.put(sku, p));
        }
        return result;
    }

    /** Locks the rows in a fixed (sorted) order so concurrent transactions cannot deadlock. */
    public Map<String, Product> lockProducts(Collection<String> skus) throws SQLException {
        Map<String, Product> result = new LinkedHashMap<>();
        for (String sku : new TreeSet<>(skus)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + PRODUCT_COLUMNS + " FROM products WHERE sku = ? FOR UPDATE")) {
                ps.setString(1, sku);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result.put(sku, product(rs));
                    }
                }
            }
        }
        return result;
    }

    public void adjustStock(String sku, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = stock + ? WHERE sku = ?")) {
            ps.setInt(1, delta);
            ps.setString(2, sku);
            ps.executeUpdate();
        }
    }

    private static Product product(ResultSet rs) throws SQLException {
        return new Product(rs.getString("sku"), rs.getString("name"), Money.round(rs.getBigDecimal("price")),
                Money.round(rs.getBigDecimal("weight_kg")), rs.getInt("stock"), rs.getBoolean("discontinued"));
    }

    // ---- customers

    public void ensureCustomer(String cpf, String email) throws SQLException {
        // atomic "insert if absent" so that two service instances can race safely
        String sql = postgres
                ? "INSERT INTO customers (cpf, email, credit_balance) VALUES (?, ?, 0) ON CONFLICT (cpf) DO NOTHING"
                : "INSERT INTO customers (cpf, email, credit_balance) SELECT CAST(? AS VARCHAR(32)), "
                + "CAST(? AS VARCHAR(255)), 0 WHERE NOT EXISTS (SELECT 1 FROM customers WHERE cpf = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cpf);
            ps.setString(2, email);
            if (!postgres) {
                ps.setString(3, cpf);
            }
            ps.executeUpdate();
        }
    }

    public Optional<Customer> customer(String cpf) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT cpf, credit_balance FROM customers WHERE cpf = ?")) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Customer(rs.getString(1), Money.round(rs.getBigDecimal(2))))
                        : Optional.empty();
            }
        }
    }

    public void addCredit(String cpf, BigDecimal delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE customers SET credit_balance = credit_balance + ? WHERE cpf = ?")) {
            ps.setBigDecimal(1, delta);
            ps.setString(2, cpf);
            ps.executeUpdate();
        }
    }

    /** True when the customer has an order that is paid, shipped or delivered. */
    public boolean hasCompletedPurchase(String cpf) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM orders WHERE cpf = ? AND status IN ('PAID', 'SHIPPED', 'DELIVERED')")) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---- orders

    public void insertOrder(Order o) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO orders (id, cpf, email, status, created_at, summary) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, o.id());
            ps.setString(2, o.cpf());
            ps.setString(3, o.email());
            ps.setString(4, o.status().name());
            ps.setObject(5, OffsetDateTime.ofInstant(o.createdAt(), ZoneOffset.UTC));
            ps.setString(6, Json.write(o.summary()));
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO order_items (order_id, line_no, sku, quantity) VALUES (?, ?, ?, ?)")) {
            int line = 0;
            for (OrderItem item : o.items()) {
                ps.setString(1, o.id());
                ps.setInt(2, line++);
                ps.setString(3, item.sku());
                ps.setInt(4, item.quantity());
                ps.executeUpdate();
            }
        }
    }

    public Optional<Order> order(String id, boolean lock) throws SQLException {
        String sql = "SELECT id, cpf, email, status, created_at, summary, tracking_code FROM orders WHERE id = ?"
                + (lock ? " FOR UPDATE" : "");
        Order header;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                header = new Order(rs.getString("id"), rs.getString("cpf"), rs.getString("email"),
                        OrderStatus.valueOf(rs.getString("status")),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        Json.read(rs.getString("summary"), Summary.class), null, rs.getString("tracking_code"));
            }
        }
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT sku, quantity FROM order_items WHERE order_id = ? ORDER BY line_no")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(rs.getString(1), rs.getInt(2)));
                }
            }
        }
        return Optional.of(new Order(header.id(), header.cpf(), header.email(), header.status(), header.createdAt(),
                header.summary(), items, header.trackingCode()));
    }

    public void updateStatus(String id, OrderStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    public void updateTrackingCode(String id, String trackingCode) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET tracking_code = ? WHERE id = ?")) {
            ps.setString(1, trackingCode);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    // ---- payment notices (idempotency)

    public Optional<String> paymentOutcome(String orderId, String transactionId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT outcome FROM payment_events WHERE order_id = ? AND transaction_id = ?")) {
            ps.setString(1, orderId);
            ps.setString(2, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public void recordPaymentOutcome(String orderId, String transactionId, String outcome) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payment_events (order_id, transaction_id, outcome) VALUES (?, ?, ?)")) {
            ps.setString(1, orderId);
            ps.setString(2, transactionId);
            ps.setString(3, outcome);
            ps.executeUpdate();
        }
    }

    // ---- notifications

    public void insertNotification(Notification n) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO notifications (id, cpf, notif_type, order_id, status) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, n.id());
            ps.setString(2, n.cpf());
            ps.setString(3, n.type().name());
            ps.setString(4, n.orderId());
            ps.setString(5, n.status());
            ps.executeUpdate();
        }
    }

    public List<Notification> notifications(String cpf) throws SQLException {
        List<Notification> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, cpf, notif_type, order_id, status FROM notifications WHERE cpf = ? ORDER BY seq")) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(notification(rs));
                }
            }
        }
        return result;
    }

    public Optional<Notification> markRead(String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE notifications SET status = 'READ' WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, cpf, notif_type, order_id, status FROM notifications WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(notification(rs)) : Optional.empty();
            }
        }
    }

    private static Notification notification(ResultSet rs) throws SQLException {
        return new Notification(rs.getString(1), rs.getString(2), NotificationType.valueOf(rs.getString(3)),
                rs.getString(4), rs.getString(5));
    }
}
