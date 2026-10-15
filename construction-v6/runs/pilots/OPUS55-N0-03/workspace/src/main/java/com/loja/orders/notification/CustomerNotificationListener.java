package com.loja.orders.notification;

import org.springframework.stereotype.Component;

import com.loja.orders.order.Order;
import com.loja.orders.order.OrderLifecycleListener;
import com.loja.orders.order.OrderStatus;

/** Avisa o cliente: por enquanto, o aviso fica na lista que o atendimento consulta. */
@Component
public class CustomerNotificationListener implements OrderLifecycleListener {

    private final NotificationRepository notifications;

    public CustomerNotificationListener(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    public void paymentApproved(Order order) {
        notifications.add(order.cpf(), order.id(), NotificationType.PAYMENT_APPROVED);
    }

    @Override
    public void shipped(Order order) {
        notifications.add(order.cpf(), order.id(), NotificationType.ORDER_SHIPPED);
    }

    @Override
    public void cancelled(Order order, OrderStatus previousStatus) {
        notifications.add(order.cpf(), order.id(), NotificationType.ORDER_CANCELLED);
    }
}
