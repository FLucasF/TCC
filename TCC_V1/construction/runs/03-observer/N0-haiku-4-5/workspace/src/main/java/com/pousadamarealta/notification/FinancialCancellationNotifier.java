package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.notification.event.ReservationCancelledEvent;
import com.pousadamarealta.notification.event.ReservationEvent;
import java.util.concurrent.atomic.AtomicLong;

public class FinancialCancellationNotifier implements ReservationEventListener {
    private final MessageStore messageStore;
    private final AtomicLong messageIdCounter;

    public FinancialCancellationNotifier(MessageStore messageStore, AtomicLong messageIdCounter) {
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

            if (res.refund == null || res.refund <= 0) {
                return;
            }

            String body = String.format(
                "Reembolso necessário:\n\n" +
                "Valor: R$ %.2f\n",
                res.refund
            ).replace(".", ",");

            Message msg = new Message(
                messageIdCounter.incrementAndGet(),
                "EMAIL",
                "financeiro@pousadamarealta.com.br",
                String.format("Reembolso - reserva %d", res.id),
                body
            );
            messageStore.addMessage(msg);
        }
    }
}
