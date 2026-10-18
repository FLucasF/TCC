package com.loja.repository;

import com.loja.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class OrderRepository {
    private final Database db;
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderRepository(Database db) {
        this.db = db;
    }

    public Order findById(String id) {
        String sql = "SELECT id, cpf, email, status, created_at, paid_at, tracking_code, summary_json, items_json " +
                     "FROM orders WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding order", e);
        }
        return null;
    }

    public void create(Order order) {
        String sql = "INSERT INTO orders (id, cpf, email, status, created_at, paid_at, tracking_code, summary_json, items_json) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, order.id);
            stmt.setString(2, order.cpf);
            stmt.setString(3, order.email);
            stmt.setString(4, order.status.name());
            stmt.setObject(5, order.createdAt);
            stmt.setObject(6, order.paidAt);
            stmt.setString(7, order.trackingCode);
            stmt.setString(8, mapper.writeValueAsString(order.summary));
            stmt.setString(9, mapper.writeValueAsString(order.items));
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error creating order", e);
        }
    }

    public void updateStatus(String id, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating order status", e);
        }
    }

    public void updateStatusAndPaidAt(String id, OrderStatus status, Instant paidAt) {
        String sql = "UPDATE orders SET status = ?, paid_at = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setObject(2, paidAt);
            stmt.setString(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating order", e);
        }
    }

    public void updateTrackingCode(String id, String trackingCode) {
        String sql = "UPDATE orders SET tracking_code = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trackingCode);
            stmt.setString(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating tracking code", e);
        }
    }

    public boolean existsWithStatus(String cpf, OrderStatus status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE cpf = ? AND status = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking order status", e);
        }
        return false;
    }

    public boolean hasCompletedOrder(String cpf) {
        String sql = "SELECT COUNT(*) FROM orders WHERE cpf = ? AND status IN ('PAID', 'SHIPPED', 'DELIVERED')";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking completed orders", e);
        }
        return false;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.id = rs.getString("id");
        order.cpf = rs.getString("cpf");
        order.email = rs.getString("email");
        order.status = OrderStatus.valueOf(rs.getString("status"));
        order.createdAt = (Instant) rs.getObject("created_at", Instant.class);
        order.paidAt = (Instant) rs.getObject("paid_at", Instant.class);
        order.trackingCode = rs.getString("tracking_code");

        try {
            order.summary = mapper.readValue(rs.getString("summary_json"), OrderSummary.class);
            order.items = mapper.readValue(rs.getString("items_json"),
                mapper.getTypeFactory().constructCollectionType(List.class, OrderItem.class));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing order JSON", e);
        }

        return order;
    }
}
