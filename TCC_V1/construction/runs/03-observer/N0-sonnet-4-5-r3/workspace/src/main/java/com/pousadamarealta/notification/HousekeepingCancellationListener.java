package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.util.FormatUtil;

public class HousekeepingCancellationListener implements EventListener<ReservationCancelledEvent> {

    private final NotificationService notificationService;
    private final String housekeepingEmail = "governanca@pousadamarealta.com.br";

    public HousekeepingCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (event.previousStatus() != ReservationStatus.CONFIRMED) {
            return;
        }

        Reservation reservation = event.reservation();

        String body = String.format("""
            Datas liberadas: %s a %s""",
            FormatUtil.formatDate(reservation.getCheckIn()),
            FormatUtil.formatDate(reservation.getCheckOut())
        );

        notificationService.sendEmail(
            housekeepingEmail,
            String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber()),
            body
        );
    }
}
