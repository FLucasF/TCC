package shop.service;

import shop.domain.NotificationType;
import shop.domain.Order;
import shop.persistence.Tx;

/** Avisos para o atendimento consultar; o envio de e-mail de verdade virá depois. */
public class NotificationListener implements OrderEventListener {
    @Override
    public void onPaid(Tx tx, Order order) {
        tx.notifications().add(order.cpf(), NotificationType.PAYMENT_APPROVED, order.id());
    }

    @Override
    public void onShipped(Tx tx, Order order) {
        tx.notifications().add(order.cpf(), NotificationType.ORDER_SHIPPED, order.id());
    }

    @Override
    public void onCancelled(Tx tx, Order order, boolean wasPaid) {
        tx.notifications().add(order.cpf(), NotificationType.ORDER_CANCELLED, order.id());
    }
}
