package br.com.pousadamarealta.service.observer;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.util.Locale;

public class FinanceNotificationObserver implements ReservationObserver {
    private final MessageRepository messageRepository;
    private static final Locale PT_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();

    public FinanceNotificationObserver(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String body = String.format("""
            Pagamento recebido para a reserva nº %d.
            Valor: %s""",
            reservation.getId(),
            formatMoney(reservation.getAmountPaid()));

        Message message = new Message(
            "EMAIL",
            "financeiro@pousadamarealta.com.br",
            String.format("Pagamento recebido - reserva %d", reservation.getId()),
            body
        );
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
        if (!wasConfirmed || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String body = String.format("""
            Reembolso a processar para a reserva nº %d.
            Valor: %s""",
            reservation.getId(),
            formatMoney(reservation.getRefund()));

        Message message = new Message(
            "EMAIL",
            "financeiro@pousadamarealta.com.br",
            String.format("Reembolso - reserva %d", reservation.getId()),
            body
        );
        messageRepository.save(message);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(PT_BR, "R$ %,.2f", amount);
    }
}
