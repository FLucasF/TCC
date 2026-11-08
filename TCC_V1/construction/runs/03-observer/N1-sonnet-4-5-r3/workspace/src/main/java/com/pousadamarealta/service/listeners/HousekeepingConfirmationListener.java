package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.DocumentFormatter;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmed;
import com.pousadamarealta.service.NotificationService;

public class HousekeepingConfirmationListener implements EventListener<ReservationConfirmed> {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final NotificationService notificationService;

    public HousekeepingConfirmationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmed event) {
        Reservation reservation = event.reservation();

        String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
        String body = String.format(
            "Entrada: %s | Saída: %s | %d %s",
            DocumentFormatter.formatDate(reservation.getCheckIn()),
            DocumentFormatter.formatDate(reservation.getCheckOut()),
            reservation.getGuests(),
            reservation.getGuests() == 1 ? "hóspede" : "hóspedes"
        );

        notificationService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }
}
