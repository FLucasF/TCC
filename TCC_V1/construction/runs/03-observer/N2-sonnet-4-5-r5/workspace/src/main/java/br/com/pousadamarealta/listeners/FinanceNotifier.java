package br.com.pousadamarealta.listeners;

import br.com.pousadamarealta.domain.*;
import br.com.pousadamarealta.domain.events.ReservationCancelled;
import br.com.pousadamarealta.domain.events.ReservationConfirmed;
import br.com.pousadamarealta.domain.events.ReservationEventListener;
import br.com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;

public final class FinanceNotifier implements ReservationEventListener {
    private final MessageRepository messageRepository;

    public FinanceNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmed event) {
        Reservation reservation = event.reservation();
        String body = String.format("""
            Pagamento recebido da reserva nº %d.

            Valor: %s
            Hóspede: %s""",
            reservation.id(),
            Formatter.formatMoney(reservation.amountPaid()),
            reservation.guestName());

        Message message = new Message(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            String.format("Pagamento recebido - reserva %d", reservation.id()),
            body
        );
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(ReservationCancelled event) {
        if (event.previousStatus() != ReservationStatus.CONFIRMED) {
            return;
        }

        Reservation reservation = event.reservation();
        BigDecimal refundAmount = reservation.refund();

        if (refundAmount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String body = String.format("""
            A reserva nº %d foi cancelada.

            Valor a reembolsar: %s
            Hóspede: %s""",
            reservation.id(),
            Formatter.formatMoney(refundAmount),
            reservation.guestName());

        Message message = new Message(
            MessageChannel.EMAIL,
            "financeiro@pousadamarealta.com.br",
            String.format("Reembolso - reserva %d", reservation.id()),
            body
        );
        messageRepository.save(message);
    }
}
