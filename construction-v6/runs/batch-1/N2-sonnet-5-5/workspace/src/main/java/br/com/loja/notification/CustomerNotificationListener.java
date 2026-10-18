package br.com.loja.notification;

import org.springframework.stereotype.Component;

import br.com.loja.order.Order;
import br.com.loja.order.OrderEventListener;

/** Registra os avisos que o atendimento consulta. (O e-mail de verdade vem depois.) */
@Component
class CustomerNotificationListener implements OrderEventListener {

    private final NotificationRepository notifications;

    CustomerNotificationListener(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public void onPaid(Order order) {
        notifications.create(order.customerCpf(), "PAYMENT_APPROVED", order.id());
    }

    @Override
    public void onShipped(Order order) {
        notifications.create(order.customerCpf(), "ORDER_SHIPPED", order.id());
    }

    @Override
    public void onCancelled(Order order, boolean wasPaid) {
        notifications.create(order.customerCpf(), "ORDER_CANCELLED", order.id());
    }
}
