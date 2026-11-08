package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.util.Formatter;

public class HousekeepingNotifier implements ReservationObserver {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final NotificationService notificationService;

    public HousekeepingNotifier(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());

        String guestsText = reservation.getGuests() == 1 ? "1 hóspede" : reservation.getGuests() + " hóspedes";

        String body = String.format(
            "Uma nova reserva foi confirmada:%n%n" +
            "Quarto: %s%n" +
            "Entrada: %s%n" +
            "Saída: %s%n" +
            "Hóspedes: %s",
            reservation.getRoomNumber(),
            Formatter.formatDate(reservation.getCheckIn()),
            Formatter.formatDate(reservation.getCheckOut()),
            guestsText
        );

        notificationService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }

        String subject = String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber());

        String body = String.format(
            "A reserva do quarto %s foi cancelada:%n%n" +
            "Datas que ficaram livres:%n" +
            "Entrada: %s%n" +
            "Saída: %s",
            reservation.getRoomNumber(),
            Formatter.formatDate(reservation.getCheckIn()),
            Formatter.formatDate(reservation.getCheckOut())
        );

        notificationService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }
}
