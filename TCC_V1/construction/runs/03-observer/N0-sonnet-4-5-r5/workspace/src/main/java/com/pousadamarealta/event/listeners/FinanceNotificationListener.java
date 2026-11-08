package com.pousadamarealta.event.listeners;

import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FinanceNotificationListener implements EventListener<ReservationConfirmedEvent> {

    private final NotificationService notificationService;
    private final DecimalFormat moneyFormatter;

    public FinanceNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        this.moneyFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        String subject = "Pagamento recebido - reserva " + event.getReservation().getId();
        String body = "R$ " + formatMoney(event.getReservation().getAmountPaid());

        notificationService.sendEmail("financeiro@pousadamarealta.com.br", subject, body);
    }

    private String formatMoney(BigDecimal amount) {
        return moneyFormatter.format(amount);
    }
}
