package com.loja.services;

import com.loja.database.Database;
import com.loja.models.Enums;
import com.loja.models.Notification;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class NotificationService {
    public static void createNotification(String cpf, Enums.NotificationType type, String orderId) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO notifications (id, cpf, type, order_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, cpf);
            pstmt.setString(3, type.name());
            pstmt.setString(4, orderId);
            pstmt.setString(5, Enums.NotificationStatus.NEW.name());
            pstmt.setObject(6, Instant.now());
            pstmt.executeUpdate();
        }
    }

    public static List<Notification> getCustomerNotifications(String cpf) throws SQLException {
        String sql = "SELECT id, type, order_id, status FROM notifications WHERE cpf = ? ORDER BY created_at ASC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cpf);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notifications.add(new Notification(
                    rs.getString("id"),
                    Enums.NotificationType.valueOf(rs.getString("type")),
                    rs.getString("order_id"),
                    Enums.NotificationStatus.valueOf(rs.getString("status"))
                ));
            }
        }
        return notifications;
    }

    public static Notification getNotification(String id) throws SQLException {
        String sql = "SELECT id, type, order_id, status FROM notifications WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Notification(
                    rs.getString("id"),
                    Enums.NotificationType.valueOf(rs.getString("type")),
                    rs.getString("order_id"),
                    Enums.NotificationStatus.valueOf(rs.getString("status"))
                );
            }
            return null;
        }
    }

    public static void markAsRead(String id) throws SQLException {
        String sql = "UPDATE notifications SET status = ? WHERE id = ?";
        try (Connection conn = Database.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, Enums.NotificationStatus.READ.name());
            pstmt.setString(2, id);
            pstmt.executeUpdate();
        }
    }
}
