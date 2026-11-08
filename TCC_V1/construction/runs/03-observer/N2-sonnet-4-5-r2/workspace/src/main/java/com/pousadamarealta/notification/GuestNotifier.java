package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationDocument;

public class GuestNotifier implements ReservationObserver {
    private final NotificationService notificationService;

    public GuestNotifier(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        ReservationDocument document = new ReservationDocument(reservation);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!%n%n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:%n%n" +
            "%s%n%n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.%n" +
            "Até breve!",
            reservation.getGuestName(),
            document.render()
        );

        notificationService.sendEmail(reservation.getGuestEmail(), subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
    }
}
