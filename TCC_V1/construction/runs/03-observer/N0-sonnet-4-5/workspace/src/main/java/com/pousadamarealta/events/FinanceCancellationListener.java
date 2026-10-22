package com.pousadamarealta.events;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceCancellationListener implements
        EventListener<ReservationCancelledEvent> {

    private final MessageRepository messageRepository;
    private static final Locale BRAZIL_LOCALE = new Locale("pt", "BR");
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    public FinanceCancellationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.reservation();

        if (reservation.getRefund() == null || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format("Valor: %s", formatCurrency(reservation.getRefund()));

        Message message = new Message("EMAIL", FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);
        return formatter.format(amount);
    }
}
