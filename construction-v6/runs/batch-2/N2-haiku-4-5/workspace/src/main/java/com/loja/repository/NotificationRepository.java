package com.loja.repository;

import com.loja.model.*;
import java.sql.*;
import java.util.*;

public class NotificationRepository {
    private final Database db;

    public NotificationRepository(Database db) {
        this.db = db;
    }

    public Notification findById(String id) {
        String sql = "SELECT id, type, order_id, cpf, status FROM notifications WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapNotification(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding notification", e);
        }
        return null;
    }

    public List<Notification> findByCpf(String cpf) {
        String sql = "SELECT id, type, order_id, cpf, status FROM notifications WHERE cpf = ? ORDER BY id ASC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding notifications", e);
        }
        return notifications;
    }

    public void create(Notification notification) {
        String sql = "INSERT INTO notifications (id, type, order_id, cpf, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, notification.id);
            stmt.setString(2, notification.type.name());
            stmt.setString(3, notification.orderId);
            stmt.setString(4, notification.cpf);
            stmt.setString(5, notification.status.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating notification", e);
        }
    }

    public void markAsRead(String id) {
        String sql = "UPDATE notifications SET status = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, NotificationStatus.READ.name());
            stmt.setString(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking notification as read", e);
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        return new Notification(
            rs.getString("id"),
            NotificationType.valueOf(rs.getString("type")),
            rs.getString("order_id"),
            rs.getString("cpf"),
            NotificationStatus.valueOf(rs.getString("status"))
        );
    }
}
