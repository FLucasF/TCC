package br.com.loja.pedidos.order;

import br.com.loja.pedidos.notification.NotificationRepository;
import br.com.loja.pedidos.notification.NotificationType;
import org.springframework.stereotype.Component;

@Component
class CustomerNotificationListener implements OrderLifecycleListener {

    private final NotificationRepository notifications;

    CustomerNotificationListener(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public void onPaid(StoredOrder order) {
        notifications.create(order.cpf(), NotificationType.PAYMENT_APPROVED, order.order().id());
    }

    @Override
    public void onShipped(StoredOrder order) {
        notifications.create(order.cpf(), NotificationType.ORDER_SHIPPED, order.order().id());
    }

    @Override
    public void onCancelled(StoredOrder order, boolean wasPaid) {
        notifications.create(order.cpf(), NotificationType.ORDER_CANCELLED, order.order().id());
    }
}
