package com.pousada.listener;

import com.pousada.domain.Reservation;
import com.pousada.event.EventListener;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEvent;
import com.pousada.service.NotificationService;

public class HousekeepingNotificationListener implements EventListener {
    private final NotificationService notificationService;

    public HousekeepingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            sendConfirmationNotification(event.getReservation());
        } else if (event instanceof ReservationCancelledEvent cancelledEvent) {
            if (cancelledEvent.wasConfirmed()) {
                sendCancellationNotification(cancelledEvent.getReservation());
            }
        }
    }

    private void sendConfirmationNotification(Reservation reservation) {
        notificationService.sendHousekeepingConfirmationEmail(reservation);
    }

    private void sendCancellationNotification(Reservation reservation) {
        notificationService.sendHousekeepingCancellationEmail(reservation);
    }
}
