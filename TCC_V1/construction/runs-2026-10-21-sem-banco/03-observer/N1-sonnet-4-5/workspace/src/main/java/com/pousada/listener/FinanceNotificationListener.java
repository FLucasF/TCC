package com.pousada.listener;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.event.EventListener;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEvent;
import com.pousada.service.NotificationService;

import java.math.BigDecimal;

public class FinanceNotificationListener implements EventListener {
    private final NotificationService notificationService;

    public FinanceNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmedEvent) {
            sendPaymentNotification(confirmedEvent);
        } else if (event instanceof ReservationCancelledEvent cancelledEvent) {
            if (cancelledEvent.wasConfirmed() && hasRefund(cancelledEvent.getRefundAmount())) {
                sendRefundNotification(cancelledEvent);
            }
        }
    }

    private void sendPaymentNotification(ReservationConfirmedEvent event) {
        notificationService.sendFinancePaymentEmail(event.getReservation(), event.getTotal());
    }

    private void sendRefundNotification(ReservationCancelledEvent event) {
        notificationService.sendFinanceRefundEmail(event.getReservation(), event.getRefundAmount());
    }

    private boolean hasRefund(Money refundAmount) {
        return refundAmount.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
