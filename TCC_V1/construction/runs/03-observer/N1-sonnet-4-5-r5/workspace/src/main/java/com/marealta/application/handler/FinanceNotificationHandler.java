package com.marealta.application.handler;

import com.marealta.domain.event.ReservationConfirmedEvent;
import com.marealta.domain.model.Reservation;
import com.marealta.infrastructure.messaging.EmailService;
import com.marealta.infrastructure.notification.EventHandler;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceNotificationHandler implements EventHandler<ReservationConfirmedEvent> {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);
    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailService emailService;

    public FinanceNotificationHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format(
            "Um pagamento foi recebido.\n\nReserva: %d\nValor: %s",
            reservation.getId(),
            formatMoney(reservation.getAmountPaid())
        );

        emailService.send(EMAIL, subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount);
    }
}
