package com.loja.repository;

import com.loja.domain.Notification;
import com.loja.domain.NotificationStatus;
import com.loja.domain.NotificationType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NotificationRepository {
    private final Database database;

    public NotificationRepository(Database database) {
        this.database = database;
    }

    public void create(String customerCpf, NotificationType type, String orderId, Connection conn) throws SQLException {
        String sql = "INSERT INTO notifications (id, customer_cpf, type, order_id, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setString(2, customerCpf);
            pstmt.setString(3, type.name());
            pstmt.setString(4, orderId);
            pstmt.setString(5, NotificationStatus.NEW.name());
            pstmt.executeUpdate();
        }
    }

    public List<Notification> findByCustomerCpf(String cpf) {
        String sql = "SELECT * FROM notifications WHERE customer_cpf = ? ORDER BY created_at ASC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notifications", e);
        }
        return notifications;
    }

    public Optional<Notification> findById(String id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notification", e);
        }
        return Optional.empty();
    }

    public void markAsRead(String id) {
        String sql = "UPDATE notifications SET status = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, NotificationStatus.READ.name());
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        return new Notification(
            rs.getString("id"),
            rs.getString("customer_cpf"),
            NotificationType.valueOf(rs.getString("type")),
            rs.getString("order_id"),
            NotificationStatus.valueOf(rs.getString("status"))
        );
    }
}
