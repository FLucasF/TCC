package br.com.loja.pedidos.notification;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.order.Order;
import br.com.loja.pedidos.order.OrderEventListener;

/** Registra os avisos que o atendimento consulta. */
@Component
class CustomerNotificationListener implements OrderEventListener {

    private final NotificationRepository notifications;

    CustomerNotificationListener(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public void paymentConfirmed(Order order) {
        notifications.create(order.cpf(), NotificationType.PAYMENT_APPROVED, order.id());
    }

    @Override
    public void shipped(Order order) {
        notifications.create(order.cpf(), NotificationType.ORDER_SHIPPED, order.id());
    }

    @Override
    public void cancelled(Order order, boolean wasPaid) {
        notifications.create(order.cpf(), NotificationType.ORDER_CANCELLED, order.id());
    }
}
