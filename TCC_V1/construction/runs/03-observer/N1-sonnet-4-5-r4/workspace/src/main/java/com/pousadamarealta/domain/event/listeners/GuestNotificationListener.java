package com.pousadamarealta.domain.event.listeners;

import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

public class GuestNotificationListener implements EventPublisher.ReservationConfirmedListener {
    private final MessageService messageService;

    public GuestNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        String receiptText = event.getReceiptText();

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receiptText
        );

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        messageService.send(message);
    }
}
