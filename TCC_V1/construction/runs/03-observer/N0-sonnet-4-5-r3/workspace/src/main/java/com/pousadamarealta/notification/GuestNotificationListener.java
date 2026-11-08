package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.util.FormatUtil;

public class GuestNotificationListener implements EventListener<ReservationConfirmedEvent> {
    private final NotificationService notificationService;

    public GuestNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String receipt = FormatUtil.formatReceipt(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        notificationService.sendEmail(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }
}
