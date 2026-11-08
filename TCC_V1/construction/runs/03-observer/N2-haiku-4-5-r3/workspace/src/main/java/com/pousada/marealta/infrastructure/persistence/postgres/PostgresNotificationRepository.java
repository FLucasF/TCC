package com.pousada.marealta.infrastructure.persistence.postgres;

import com.pousada.marealta.domain.notification.PersistedNotification;
import com.pousada.marealta.domain.repository.NotificationRepository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostgresNotificationRepository implements NotificationRepository {
    private final DataSource dataSource;

    public PostgresNotificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(PersistedNotification notification) {
        if (notification == null) {
            return;
        }

        String sql = "INSERT INTO notifications (id, reservation_id, channel, recipient, subject, body, success, error_message) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, notification.id());
            stmt.setLong(2, notification.reservationId());
            stmt.setString(3, notification.channel());
            stmt.setString(4, notification.recipient());
            stmt.setString(5, notification.subject());
            stmt.setString(6, notification.body());
            stmt.setBoolean(7, notification.success());
            stmt.setString(8, notification.errorMessage());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving notification", e);
        }
    }

    @Override
    public List<PersistedNotification> findAll() {
        String sql = "SELECT id, reservation_id, channel, recipient, subject, body, success, error_message, sent_at " +
                "FROM notifications ORDER BY sent_at DESC";
        List<PersistedNotification> notifications = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                notifications.add(mapRowToNotification(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying notifications", e);
        }

        return notifications;
    }

    @Override
    public List<PersistedNotification> findByReservationId(long reservationId) {
        String sql = "SELECT id, reservation_id, channel, recipient, subject, body, success, error_message, sent_at " +
                "FROM notifications WHERE reservation_id = ? ORDER BY sent_at DESC";
        List<PersistedNotification> notifications = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, reservationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRowToNotification(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying notifications for reservation", e);
        }

        return notifications;
    }

    private PersistedNotification mapRowToNotification(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        long reservationId = rs.getLong("reservation_id");
        String channel = rs.getString("channel");
        String recipient = rs.getString("recipient");
        String subject = rs.getString("subject");
        String body = rs.getString("body");
        boolean success = rs.getBoolean("success");
        String errorMessage = rs.getString("error_message");
        LocalDateTime sentAt = rs.getTimestamp("sent_at").toLocalDateTime();

        return new PersistedNotification(id, reservationId, channel, recipient, subject, body, success, errorMessage, sentAt);
    }
}
