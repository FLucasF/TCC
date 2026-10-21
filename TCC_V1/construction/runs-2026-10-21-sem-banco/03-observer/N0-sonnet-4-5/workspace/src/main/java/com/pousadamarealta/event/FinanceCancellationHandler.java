package com.pousadamarealta.event;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.MessageService;

import java.math.BigDecimal;

public class FinanceCancellationHandler implements EventHandler<ReservationCancelledEvent> {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final MessageService messageService;

    public FinanceCancellationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        Reservation r = event.getReservation();

        if (r.getRefund() == null || r.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = String.format("Reembolso - reserva %d", r.getId());
        String body = String.format("Valor a devolver: %s", formatMoney(r.getRefund()));

        messageService.send(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
