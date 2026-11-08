package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.DocumentFormatter;

public final class GuestNotificationListener implements ReservationEventListener {
    private final MessageService messageService;

    public GuestNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, RoomType roomType) {
        String receipt = DocumentFormatter.formatReceipt(reservation, roomType);
        String body = String.format(
            "Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.guestName(),
            receipt
        );

        messageService.send(
            MessageChannel.EMAIL,
            reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    @Override
    public void onReservationCancelled(Reservation reservation, RoomType roomType, boolean wasPreviouslyConfirmed) {
    }
}
