package com.loja.domain.observers;

import com.loja.domain.entities.Notification;
import com.loja.domain.entities.NotificationStatus;
import com.loja.domain.entities.NotificationType;
import com.loja.domain.entities.Order;
import com.loja.domain.events.PaymentConfirmedEvent;
import com.loja.repositories.NotificationRepository;

public class NotificationObserver implements PaymentConfirmedObserver {
    private final NotificationRepository notificationRepository;

    public NotificationObserver(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        Order order = event.getOrder();

        Notification notification = new Notification(
                0,
                NotificationType.PAYMENT_APPROVED,
                order.getId(),
                NotificationStatus.NEW
        );

        notificationRepository.save(order.getCustomerCpf(), notification);
    }
}
