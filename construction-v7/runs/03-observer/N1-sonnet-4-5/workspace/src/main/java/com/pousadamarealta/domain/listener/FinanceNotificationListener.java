package com.pousadamarealta.domain.listener;

import com.pousadamarealta.domain.event.Event;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

public class FinanceNotificationListener implements Consumer<Event> {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final MessageService messageService;

    public FinanceNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void accept(Event event) {
        if (event instanceof ReservationConfirmedEvent confirmedEvent) {
            sendPaymentNotification(confirmedEvent.reservation());
        } else if (event instanceof ReservationCancelledEvent cancelledEvent) {
            if (cancelledEvent.wasConfirmed() &&
                cancelledEvent.reservation().getRefund().compareTo(BigDecimal.ZERO) > 0) {
                sendRefundNotification(cancelledEvent.reservation());
            }
        }
    }

    private void sendPaymentNotification(Reservation reservation) {
        String body = String.format(
            "Valor pago: %s",
            CURRENCY_FORMAT.format(reservation.getAmountPaid())
        );

        messageService.sendEmail(
            FINANCE_EMAIL,
            String.format("Pagamento recebido - reserva %d", reservation.getId()),
            body
        );
    }

    private void sendRefundNotification(Reservation reservation) {
        String body = String.format(
            "Valor a devolver: %s",
            CURRENCY_FORMAT.format(reservation.getRefund())
        );

        messageService.sendEmail(
            FINANCE_EMAIL,
            String.format("Reembolso - reserva %d", reservation.getId()),
            body
        );
    }
}
