package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.notification.event.ReservationConfirmedEvent;
import com.pousadamarealta.notification.event.ReservationEvent;
import com.pousadamarealta.util.ComprovantFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class GuestConfirmationNotifier implements ReservationEventListener {
    private final MessageStore messageStore;
    private final AtomicLong messageIdCounter;

    public GuestConfirmationNotifier(MessageStore messageStore, AtomicLong messageIdCounter) {
        this.messageStore = messageStore;
        this.messageIdCounter = messageIdCounter;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            Reservation res = confirmed.reservation();
            String comprovante = ComprovantFormatter.format(res);
            String body = String.format(
                "Olá, %s!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                "%s\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!\n",
                res.guestName,
                comprovante
            );

            Message msg = new Message(
                messageIdCounter.incrementAndGet(),
                "EMAIL",
                res.guestEmail,
                "Reserva confirmada - Pousada Maré Alta",
                body
            );
            messageStore.addMessage(msg);
        }
    }
}
