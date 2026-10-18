package br.com.loja.pedidos.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

    private static final String COLUMNS = "id, type, order_id, status";

    private final JdbcClient jdbc;

    public NotificationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String cpf, NotificationType type, String orderId) {
        jdbc.sql("INSERT INTO notifications (id, cpf, type, order_id, status) VALUES (?, ?, ?, ?, 'NEW')")
                .param(UUID.randomUUID().toString()).param(cpf).param(type.name()).param(orderId)
                .update();
    }

    public List<Notification> listByCustomer(String cpf) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM notifications WHERE cpf = ? ORDER BY seq")
                .param(cpf)
                .query(this::map)
                .list();
    }

    public Optional<Notification> find(String id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM notifications WHERE id = ?")
                .param(id)
                .query(this::map)
                .optional();
    }

    public void markRead(String id) {
        jdbc.sql("UPDATE notifications SET status = 'READ' WHERE id = ?").param(id).update();
    }

    private Notification map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Notification(rs.getString("id"), NotificationType.valueOf(rs.getString("type")),
                rs.getString("order_id"), rs.getString("status"));
    }
}
