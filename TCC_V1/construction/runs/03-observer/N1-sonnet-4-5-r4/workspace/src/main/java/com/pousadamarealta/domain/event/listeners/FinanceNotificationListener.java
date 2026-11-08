package com.pousadamarealta.domain.event.listeners;

import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class FinanceNotificationListener implements
        EventPublisher.ReservationConfirmedListener,
        EventPublisher.ReservationCancelledListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private static final Locale LOCALE_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();

    private final MessageService messageService;

    public FinanceNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();

        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format(
            "Valor: %s",
            formatMoney(reservation.getAmountPaid())
        );

        Message message = new Message("EMAIL", FINANCE_EMAIL, subject, body);
        messageService.send(message);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.getReservation();

        if (reservation.getRefund() == null || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format(
            "Valor: %s",
            formatMoney(reservation.getRefund())
        );

        Message message = new Message("EMAIL", FINANCE_EMAIL, subject, body);
        messageService.send(message);
    }

    private String formatMoney(BigDecimal amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(LOCALE_BR);
        return currencyFormatter.format(amount);
    }
}
