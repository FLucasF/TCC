package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.notification.event.ReservationCancelledEvent;
import com.pousadamarealta.notification.event.ReservationEvent;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class GovernanceCancellationNotifier implements ReservationEventListener {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final MessageStore messageStore;
    private final AtomicLong messageIdCounter;

    public GovernanceCancellationNotifier(MessageStore messageStore, AtomicLong messageIdCounter) {
        this.messageStore = messageStore;
        this.messageIdCounter = messageIdCounter;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationCancelledEvent cancelled) {
            Reservation res = cancelled.reservation();
            ReservationStatus previousStatus = cancelled.previousStatus();

            if (previousStatus != ReservationStatus.CONFIRMED && previousStatus != ReservationStatus.CHECKED_IN) {
                return;
            }

            String body = String.format(
                "Reserva cancelada:\n\n" +
                "Quarto: %d\n" +
                "Datas liberadas: %s a %s\n",
                res.roomNumber,
                res.checkIn.format(DATE_FORMATTER),
                res.checkOut.format(DATE_FORMATTER)
            );

            Message msg = new Message(
                messageIdCounter.incrementAndGet(),
                "EMAIL",
                "governanca@pousadamarealta.com.br",
                String.format("Reserva cancelada - quarto %d", res.roomNumber),
                body
            );
            messageStore.addMessage(msg);
        }
    }
}
