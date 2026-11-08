package com.marealta.application.handler;

import com.marealta.domain.event.ReservationCancelledEvent;
import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.ReservationStatus;
import com.marealta.infrastructure.messaging.EmailService;
import com.marealta.infrastructure.notification.EventHandler;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceCancellationHandler implements EventHandler<ReservationCancelledEvent> {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);
    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailService emailService;

    public FinanceCancellationHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (event.previousStatus() != ReservationStatus.CONFIRMED) {
            return;
        }

        Reservation reservation = event.reservation();
        if (reservation.getRefund() == null || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format(
            "Um reembolso deve ser processado.\n\nReserva: %d\nValor: %s",
            reservation.getId(),
            formatMoney(reservation.getRefund())
        );

        emailService.send(EMAIL, subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount);
    }
}
