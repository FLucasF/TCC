package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.notification.event.ReservationConfirmedEvent;
import com.pousadamarealta.notification.event.ReservationEvent;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class GovernanceConfirmationNotifier implements ReservationEventListener {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final MessageStore messageStore;
    private final AtomicLong messageIdCounter;

    public GovernanceConfirmationNotifier(MessageStore messageStore, AtomicLong messageIdCounter) {
        this.messageStore = messageStore;
        this.messageIdCounter = messageIdCounter;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            Reservation res = confirmed.reservation();
            String body = String.format(
                "Reserva confirmada:\n\n" +
                "Quarto: %d\n" +
                "Entrada: %s\n" +
                "Saída: %s\n" +
                "Hóspedes: %d\n",
                res.roomNumber,
                res.checkIn.format(DATE_FORMATTER),
                res.checkOut.format(DATE_FORMATTER),
                res.guests
            );

            Message msg = new Message(
                messageIdCounter.incrementAndGet(),
                "EMAIL",
                "governanca@pousadamarealta.com.br",
                String.format("Reserva confirmada - quarto %d", res.roomNumber),
                body
            );
            messageStore.addMessage(msg);
        }
    }
}
