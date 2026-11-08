package com.pousadamarealta.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.events.ReservationEventListener;
import com.pousadamarealta.services.DocumentService;
import com.pousadamarealta.services.MessageService;
import com.pousadamarealta.domain.MessageChannel;

public class GuestNotificationListener implements ReservationEventListener {
    private final MessageService messageService;
    private final DocumentService documentService;

    public GuestNotificationListener(MessageService messageService, DocumentService documentService) {
        this.messageService = messageService;
        this.documentService = documentService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        var document = documentService.createDocument(reservation);
        var receiptText = documentService.generateReceiptText(document);

        String body = String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receiptText
        );

        messageService.sendMessage(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
    }
}
