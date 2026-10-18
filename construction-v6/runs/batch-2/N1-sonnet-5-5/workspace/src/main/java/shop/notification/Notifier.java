package shop.notification;

import shop.order.Order;
import shop.persistence.Tx;

import java.util.UUID;

/** Registers a notice for the customer in the list the support team reads. */
public final class Notifier {
    public void notify(Tx tx, Order order, NotificationType type) {
        tx.notifications().insert(new Notification(
                UUID.randomUUID().toString(), type, order.id(), NotificationStatus.NEW, order.customerCpf()));
    }
}
