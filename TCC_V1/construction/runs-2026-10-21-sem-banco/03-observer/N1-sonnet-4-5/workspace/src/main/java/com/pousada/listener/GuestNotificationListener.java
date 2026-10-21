package com.pousada.listener;

import com.pousada.domain.Reservation;
import com.pousada.event.EventListener;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.event.ReservationEvent;
import com.pousada.service.NotificationService;

public class GuestNotificationListener implements EventListener {
    private final NotificationService notificationService;

    public GuestNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmedEvent) {
            sendConfirmationEmail(confirmedEvent);
        }
    }

    private void sendConfirmationEmail(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        notificationService.sendGuestConfirmationEmail(reservation, event.getTotal());
    }
}
