package com.pousadamarealta.events;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceNotificationListener implements
        EventListener<ReservationConfirmedEvent> {

    private final MessageRepository messageRepository;
    private static final Locale BRAZIL_LOCALE = new Locale("pt", "BR");
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    public FinanceNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();

        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format("Valor: %s", formatCurrency(reservation.getAmountPaid()));

        Message message = new Message("EMAIL", FINANCE_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(BRAZIL_LOCALE);
        return formatter.format(amount);
    }
}
