package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.DocumentFormatter;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationCancelled;
import com.pousadamarealta.service.NotificationService;

public class HousekeepingCancellationListener implements EventListener<ReservationCancelled> {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final NotificationService notificationService;

    public HousekeepingCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelled event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.reservation();

        String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
        String body = String.format(
            "Datas liberadas: %s a %s",
            DocumentFormatter.formatDate(reservation.getCheckIn()),
            DocumentFormatter.formatDate(reservation.getCheckOut())
        );

        notificationService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }
}
