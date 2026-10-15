package com.loja.orders.notification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

    private final JdbcClient jdbc;

    public NotificationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void add(String cpf, String orderId, NotificationType type) {
        jdbc.sql("""
                INSERT INTO notification (id, cpf, order_id, type, status)
                VALUES (:id, :cpf, :orderId, :type, :status)
                """)
                .param("id", UUID.randomUUID().toString())
                .param("cpf", cpf)
                .param("orderId", orderId)
                .param("type", type.name())
                .param("status", NotificationStatus.NEW.name())
                .update();
    }

    /** Os avisos do cliente, do mais antigo para o mais novo. */
    public List<Notification> findByCpf(String cpf) {
        return jdbc.sql("SELECT * FROM notification WHERE cpf = :cpf ORDER BY seq")
                .param("cpf", cpf)
                .query(NotificationRepository::map)
                .list();
    }

    public Optional<Notification> markRead(String id) {
        jdbc.sql("UPDATE notification SET status = :status WHERE id = :id")
                .param("status", NotificationStatus.READ.name())
                .param("id", id)
                .update();
        return jdbc.sql("SELECT * FROM notification WHERE id = :id")
                .param("id", id)
                .query(NotificationRepository::map)
                .optional();
    }

    private static Notification map(ResultSet rs, int rowNum) throws SQLException {
        return new Notification(
                rs.getString("id"),
                NotificationType.valueOf(rs.getString("type")),
                rs.getString("order_id"),
                NotificationStatus.valueOf(rs.getString("status")));
    }
}
