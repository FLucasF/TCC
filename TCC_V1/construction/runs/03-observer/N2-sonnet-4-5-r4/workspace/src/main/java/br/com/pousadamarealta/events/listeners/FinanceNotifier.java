package br.com.pousadamarealta.events.listeners;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.events.ReservationCancelledEvent;
import br.com.pousadamarealta.events.ReservationConfirmedEvent;
import br.com.pousadamarealta.events.ReservationEvent;
import br.com.pousadamarealta.events.ReservationEventListener;
import br.com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;

public class FinanceNotifier implements ReservationEventListener {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public FinanceNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            handleConfirmed(confirmed);
        } else if (event instanceof ReservationCancelledEvent cancelled) {
            handleCancelled(cancelled);
        }
    }

    private void handleConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        String subject = "Pagamento recebido - reserva " + reservation.getId();
        String body = "Valor: " + reservation.formatMoney(event.getAmountPaid());

        Message message = new Message("EMAIL", FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private void handleCancelled(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        if (event.getRefundAmount().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Reservation reservation = event.getReservation();
        String subject = "Reembolso - reserva " + reservation.getId();
        String body = "Valor: " + reservation.formatMoney(event.getRefundAmount());

        Message message = new Message("EMAIL", FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }
}
