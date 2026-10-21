package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.notification.event.ReservationConfirmedEvent;
import com.pousadamarealta.notification.event.ReservationEvent;
import java.util.concurrent.atomic.AtomicLong;

public class FinancialConfirmationNotifier implements ReservationEventListener {
    private final MessageStore messageStore;
    private final AtomicLong messageIdCounter;

    public FinancialConfirmationNotifier(MessageStore messageStore, AtomicLong messageIdCounter) {
        this.messageStore = messageStore;
        this.messageIdCounter = messageIdCounter;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            Reservation res = confirmed.reservation();
            String body = String.format(
                "Pagamento recebido:\n\n" +
                "Valor: R$ %.2f\n",
                res.total
            ).replace(".", ",");

            Message msg = new Message(
                messageIdCounter.incrementAndGet(),
                "EMAIL",
                "financeiro@pousadamarealta.com.br",
                String.format("Pagamento recebido - reserva %d", res.id),
                body
            );
            messageStore.addMessage(msg);
        }
    }
}
