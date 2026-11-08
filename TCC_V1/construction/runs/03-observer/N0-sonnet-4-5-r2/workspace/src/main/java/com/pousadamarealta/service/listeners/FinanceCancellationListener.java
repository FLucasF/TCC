package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

import java.math.BigDecimal;

public class FinanceCancellationListener implements
    EventListener<ReservationCancelledEvent> {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageService messageService;

    public FinanceCancellationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation r = event.getReservation();

        if (r.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = "Reembolso - reserva " + r.getId();
        String body = String.format(
            "Um reembolso deve ser processado:\n\n" +
            "Reserva: %d\n" +
            "Valor a devolver: %s",
            r.getId(),
            Formatter.formatMoney(r.getRefund())
        );

        messageService.sendEmail(FINANCE_EMAIL, subject, body);
    }
}
