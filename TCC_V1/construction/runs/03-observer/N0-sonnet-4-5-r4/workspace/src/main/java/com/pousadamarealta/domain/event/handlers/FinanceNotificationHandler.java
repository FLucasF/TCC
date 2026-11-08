package com.pousadamarealta.domain.event.handlers;

import com.pousadamarealta.domain.event.EventHandler;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.util.MoneyFormatter;

import java.math.BigDecimal;

public class FinanceNotificationHandler {
    private final MessageService messageService;

    public FinanceNotificationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public EventHandler<ReservationConfirmedEvent> onConfirmed() {
        return event -> {
            Reservation r = event.reservation();
            String body = String.format(
                "Valor pago: %s",
                MoneyFormatter.format(r.getAmountPaid())
            );

            Message message = new Message(
                "EMAIL",
                "financeiro@pousadamarealta.com.br",
                "Pagamento recebido - reserva " + r.getId(),
                body
            );

            messageService.send(message);
        };
    }

    public EventHandler<ReservationCancelledEvent> onCancelled() {
        return event -> {
            if (!event.wasConfirmed()) {
                return;
            }

            Reservation r = event.reservation();
            BigDecimal refund = r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO;

            if (refund.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }

            String body = String.format(
                "Valor a devolver: %s",
                MoneyFormatter.format(refund)
            );

            Message message = new Message(
                "EMAIL",
                "financeiro@pousadamarealta.com.br",
                "Reembolso - reserva " + r.getId(),
                body
            );

            messageService.send(message);
        };
    }
}
