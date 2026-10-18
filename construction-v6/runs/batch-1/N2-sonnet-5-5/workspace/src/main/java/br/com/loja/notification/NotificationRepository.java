package br.com.loja.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

    private final JdbcClient jdbc;

    NotificationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String cpf, String type, String orderId) {
        jdbc.sql("INSERT INTO notifications (customer_cpf, type, order_id, status) VALUES (:cpf, :type, :order, 'NEW')")
                .param("cpf", cpf).param("type", type).param("order", orderId).update();
    }

    public List<Notification> findByCustomer(String cpf) {
        return jdbc.sql("SELECT id, type, order_id, status FROM notifications WHERE customer_cpf = :cpf ORDER BY id")
                .param("cpf", cpf).query(this::map).list();
    }

    public Optional<Notification> markRead(long id) {
        jdbc.sql("UPDATE notifications SET status = 'READ' WHERE id = :id").param("id", id).update();
        return jdbc.sql("SELECT id, type, order_id, status FROM notifications WHERE id = :id")
                .param("id", id).query(this::map).optional();
    }

    private Notification map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Notification(rs.getLong("id"), rs.getString("type"), rs.getString("order_id"),
                rs.getString("status"));
    }
}
