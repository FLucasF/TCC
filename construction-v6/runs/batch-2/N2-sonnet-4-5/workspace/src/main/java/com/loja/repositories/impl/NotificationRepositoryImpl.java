package com.loja.repositories.impl;

import com.loja.domain.entities.Notification;
import com.loja.domain.entities.NotificationStatus;
import com.loja.domain.entities.NotificationType;
import com.loja.infrastructure.Database;
import com.loja.repositories.NotificationRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepositoryImpl implements NotificationRepository {
    private final Database database;

    public NotificationRepositoryImpl(Database database) {
        this.database = database;
    }

    @Override
    public void save(String customerCpf, Notification notification) {
        String sql = "INSERT INTO notifications (customer_cpf, type, order_id, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerCpf);
            stmt.setString(2, notification.getType().name());
            stmt.setString(3, notification.getOrderId());
            stmt.setString(4, notification.getStatus().name());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save notification", e);
        }
    }

    @Override
    public List<Notification> findByCustomerCpf(String customerCpf) {
        String sql = "SELECT * FROM notifications WHERE customer_cpf = ? ORDER BY id ASC";
        List<Notification> notifications = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, customerCpf);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                notifications.add(new Notification(
                        rs.getLong("id"),
                        NotificationType.valueOf(rs.getString("type")),
                        rs.getString("order_id"),
                        NotificationStatus.valueOf(rs.getString("status"))
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notifications for customer: " + customerCpf, e);
        }

        return notifications;
    }

    @Override
    public Notification findById(long id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Notification(
                        rs.getLong("id"),
                        NotificationType.valueOf(rs.getString("type")),
                        rs.getString("order_id"),
                        NotificationStatus.valueOf(rs.getString("status"))
                );
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find notification: " + id, e);
        }
    }

    @Override
    public void update(Notification notification) {
        String sql = "UPDATE notifications SET status = ? WHERE id = ?";
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, notification.getStatus().name());
            stmt.setLong(2, notification.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update notification: " + notification.getId(), e);
        }
    }
}
