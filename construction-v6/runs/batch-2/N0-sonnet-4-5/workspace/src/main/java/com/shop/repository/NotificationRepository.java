package com.shop.repository;

import com.shop.config.Database;
import com.shop.model.Notification;
import com.shop.model.enums.NotificationStatus;
import com.shop.model.enums.NotificationType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private final Database database;

    public NotificationRepository(Database database) {
        this.database = database;
    }

    public Notification findById(Long id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapNotification(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notification", e);
        }
    }

    public List<Notification> findByCustomerCpf(String cpf) {
        String sql = "SELECT * FROM notifications WHERE customer_cpf = ? ORDER BY id ASC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notifications", e);
        }
        return notifications;
    }

    public void create(Connection conn, NotificationType type, Long orderId, String customerCpf) throws SQLException {
        String sql = """
            INSERT INTO notifications (type, order_id, customer_cpf, status)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            stmt.setLong(2, orderId);
            stmt.setString(3, customerCpf);
            stmt.setString(4, NotificationStatus.NEW.name());
            stmt.executeUpdate();
        }
    }

    public void markAsRead(Long id) {
        String sql = "UPDATE notifications SET status = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, NotificationStatus.READ.name());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setType(NotificationType.valueOf(rs.getString("type")));
        notification.setOrderId(rs.getLong("order_id"));
        notification.setCustomerCpf(rs.getString("customer_cpf"));
        notification.setStatus(NotificationStatus.valueOf(rs.getString("status")));
        return notification;
    }
}
