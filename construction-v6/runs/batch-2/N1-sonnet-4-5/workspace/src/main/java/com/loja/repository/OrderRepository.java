package com.loja.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.domain.*;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class OrderRepository {
    private final Database database;
    private final ObjectMapper objectMapper;

    public OrderRepository(Database database, ObjectMapper objectMapper) {
        this.database = database;
        this.objectMapper = objectMapper;
    }

    public void save(Order order, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO orders (id, customer_cpf, customer_email, zip_code, region, shipping_method,
                               coupon, payment_method, club_tier, status, created_at, tracking_code,
                               payment_transaction_id, summary_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::json)
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order.getId());
            pstmt.setString(2, order.getCustomerCpf());
            pstmt.setString(3, order.getCustomerEmail());
            pstmt.setString(4, order.getZipCode());
            pstmt.setString(5, order.getRegion().name());
            pstmt.setString(6, order.getShippingMethod().name());
            pstmt.setString(7, order.getCoupon());
            pstmt.setString(8, order.getPaymentMethod().name());
            pstmt.setString(9, order.getClubTier().name());
            pstmt.setString(10, order.getStatus().name());
            pstmt.setTimestamp(11, Timestamp.from(order.getCreatedAt()));
            pstmt.setString(12, order.getTrackingCode());
            pstmt.setString(13, order.getPaymentTransactionId());
            pstmt.setString(14, objectMapper.writeValueAsString(order.getSummary()));
            pstmt.executeUpdate();
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize summary", e);
        }

        String itemSql = "INSERT INTO order_items (order_id, sku, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(itemSql)) {
            for (OrderItem item : order.getItems()) {
                pstmt.setString(1, order.getId());
                pstmt.setString(2, item.getSku());
                pstmt.setInt(3, item.getQuantity());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public Optional<Order> findById(String id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    loadItems(order, conn);
                    return Optional.of(order);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find order", e);
        }
        return Optional.empty();
    }

    public Optional<Order> findByIdForUpdate(String id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ? FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    loadItems(order, conn);
                    return Optional.of(order);
                }
            }
        }
        return Optional.empty();
    }

    public void updateStatus(String orderId, OrderStatus status, Connection conn) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        }
    }

    public void updateTrackingCode(String orderId, String trackingCode, Connection conn) throws SQLException {
        String sql = "UPDATE orders SET tracking_code = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trackingCode);
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        }
    }

    public void updatePaymentTransaction(String orderId, String transactionId, Connection conn) throws SQLException {
        String sql = "UPDATE orders SET payment_transaction_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        }
    }

    public boolean hasCompletedOrders(String cpf, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE customer_cpf = ? AND status IN ('PAID', 'SHIPPED', 'DELIVERED')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public boolean recordPaymentTransaction(String orderId, String transactionId, Instant paidAt, Connection conn) throws SQLException {
        String sql = "INSERT INTO payment_transactions (order_id, transaction_id, paid_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.setString(2, transactionId);
            pstmt.setTimestamp(3, Timestamp.from(paidAt));
            int inserted = pstmt.executeUpdate();
            return inserted > 0;
        }
    }

    private void loadItems(Order order, Connection conn) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(new OrderItem(
                        rs.getString("sku"),
                        rs.getInt("quantity")
                    ));
                }
                order.setItems(items);
            }
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getString("id"));
        order.setCustomerCpf(rs.getString("customer_cpf"));
        order.setCustomerEmail(rs.getString("customer_email"));
        order.setZipCode(rs.getString("zip_code"));
        order.setRegion(Region.valueOf(rs.getString("region")));
        order.setShippingMethod(ShippingMethod.valueOf(rs.getString("shipping_method")));
        order.setCoupon(rs.getString("coupon"));
        order.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        order.setClubTier(ClubTier.valueOf(rs.getString("club_tier")));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        order.setTrackingCode(rs.getString("tracking_code"));
        order.setPaymentTransactionId(rs.getString("payment_transaction_id"));

        try {
            String summaryJson = rs.getString("summary_json");
            CheckoutSummary summary = objectMapper.readValue(summaryJson, CheckoutSummary.class);
            order.setSummary(summary);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize summary", e);
        }

        return order;
    }
}
