package com.pousadamarealta.domain.event.handlers;

import com.pousadamarealta.domain.event.EventHandler;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.util.DocumentFormatter;

public class GuestNotificationHandler implements EventHandler<ReservationConfirmedEvent> {
    private final MessageService messageService;

    public GuestNotificationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String receipt = DocumentFormatter.formatReceipt(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageService.send(message);
    }
}
