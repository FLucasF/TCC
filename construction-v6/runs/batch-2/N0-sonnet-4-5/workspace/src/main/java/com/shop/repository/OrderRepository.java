package com.shop.repository;

import com.shop.config.Database;
import com.shop.dto.CheckoutSummary;
import com.shop.model.Order;
import com.shop.model.OrderItem;
import com.shop.model.enums.OrderStatus;

import java.sql.*;
import java.time.Instant;
import java.util.List;

public class OrderRepository {
    private final Database database;

    public OrderRepository(Database database) {
        this.database = database;
    }

    public Order findById(Long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order", e);
        }
    }

    public Order findByIdForUpdate(Connection conn, Long id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
                return null;
            }
        }
    }

    public Long create(Connection conn, String customerCpf, CheckoutSummary summary, List<OrderItem> items) throws SQLException {
        String sql = """
            INSERT INTO orders (customer_cpf, status, created_at, summary, items)
            VALUES (?, ?, ?, ?::jsonb, ?::jsonb)
            RETURNING id
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerCpf);
            stmt.setString(2, OrderStatus.AWAITING_PAYMENT.name());
            stmt.setTimestamp(3, Timestamp.from(Instant.now()));
            stmt.setObject(4, Database.createJsonb(summary));
            stmt.setObject(5, Database.createJsonb(items));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to get order ID");
            }
        }
    }

    public void updateStatus(Connection conn, Long id, OrderStatus status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateTrackingCode(Connection conn, Long id, String trackingCode) throws SQLException {
        String sql = "UPDATE orders SET tracking_code = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trackingCode);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updatePaymentTransaction(Connection conn, Long id, String transactionId) throws SQLException {
        String sql = "UPDATE orders SET last_payment_transaction_id = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionId);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public long countPaidOrdersByCpf(Connection conn, String cpf) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM orders
            WHERE customer_cpf = ? AND status IN ('PAID', 'SHIPPED', 'DELIVERED')
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setCustomerCpf(rs.getString("customer_cpf"));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        order.setSummary(Database.getSummaryFromResultSet(rs, "summary"));
        order.setItems(Database.getItemsFromResultSet(rs, "items"));
        order.setTrackingCode(rs.getString("tracking_code"));
        order.setLastPaymentTransactionId(rs.getString("last_payment_transaction_id"));
        return order;
    }
}
