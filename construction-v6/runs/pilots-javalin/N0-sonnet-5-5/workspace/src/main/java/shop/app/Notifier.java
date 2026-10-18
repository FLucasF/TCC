package shop.app;

import java.sql.SQLException;
import java.util.UUID;
import shop.db.Dao;
import shop.domain.Notification;
import shop.domain.NotificationType;
import shop.domain.Order;

/** Records customer notices for the support team (real e-mail comes later). */
public final class Notifier {
    public void notify(Dao dao, Order order, NotificationType type) throws SQLException {
        dao.insertNotification(new Notification(UUID.randomUUID().toString(), order.cpf(), type, order.id(), "NEW"));
    }
}
