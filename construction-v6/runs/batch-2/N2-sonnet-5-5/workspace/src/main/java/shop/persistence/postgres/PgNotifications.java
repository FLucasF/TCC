package shop.persistence.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import shop.domain.Notification;
import shop.domain.NotificationStatus;
import shop.domain.NotificationType;
import shop.persistence.NotificationRepository;

class PgNotifications implements NotificationRepository {
    private static final String COLUMNS = "id, type, order_id, status, cpf";

    private final Connection c;

    PgNotifications(Connection c) {
        this.c = c;
    }

    @Override
    public void add(String cpf, NotificationType type, String orderId) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO notifications(id, cpf, type, order_id, status) VALUES (?, ?, ?, ?, 'NEW')")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, cpf);
                ps.setString(3, type.name());
                ps.setString(4, orderId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public List<Notification> findByCpf(String cpf) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT " + COLUMNS + " FROM notifications WHERE cpf = ? ORDER BY seq")) {
                ps.setString(1, cpf);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Notification> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(map(rs));
                    }
                    return result;
                }
            }
        });
    }

    @Override
    public Optional<Notification> find(String id) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM notifications WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void markRead(String id) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE notifications SET status = 'READ' WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private static Notification map(ResultSet rs) throws SQLException {
        return new Notification(rs.getString("id"), NotificationType.valueOf(rs.getString("type")),
                rs.getString("order_id"), NotificationStatus.valueOf(rs.getString("status")), rs.getString("cpf"));
    }
}
