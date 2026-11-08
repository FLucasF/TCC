package com.pousadamarealta.application.notification;

import com.pousadamarealta.domain.event.EventListener;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.model.Reservation;
import java.math.BigDecimal;
import java.util.Locale;

public class FinanceCancellationListener implements EventListener<ReservationCancelledEvent> {

    private final NotificationService notificationService;
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public FinanceCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.reservation();
        BigDecimal refund = reservation.getRefund();

        if (refund == null || refund.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = "Reembolso - reserva " + reservation.getId();
        String body = buildRefundBody(refund);
        notificationService.sendEmail(FINANCE_EMAIL, subject, body);
    }

    private String buildRefundBody(BigDecimal amount) {
        return String.format("Valor a devolver: %s", formatMoney(amount));
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(BR_LOCALE, "R$ %,.2f", amount);
    }
}
