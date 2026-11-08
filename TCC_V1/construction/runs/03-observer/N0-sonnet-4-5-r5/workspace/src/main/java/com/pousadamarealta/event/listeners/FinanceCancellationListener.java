package com.pousadamarealta.event.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.service.NotificationService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FinanceCancellationListener implements EventListener<ReservationCancelledEvent> {

    private final NotificationService notificationService;
    private final DecimalFormat moneyFormatter;

    public FinanceCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        this.moneyFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.getReservation();

        if (reservation.getRefund() == null || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = "Reembolso - reserva " + reservation.getId();
        String body = "R$ " + formatMoney(reservation.getRefund());

        notificationService.sendEmail("financeiro@pousadamarealta.com.br", subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        return moneyFormatter.format(amount);
    }
}
