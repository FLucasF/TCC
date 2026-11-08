package com.pousadamarealta.application.notification;

import com.pousadamarealta.domain.event.EventListener;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import java.math.BigDecimal;
import java.util.Locale;

public class FinanceNotificationListener implements EventListener<ReservationConfirmedEvent> {

    private final NotificationService notificationService;
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public FinanceNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Pagamento recebido - reserva " + reservation.getId();
        String body = buildPaymentBody(reservation.getAmountPaid());
        notificationService.sendEmail(FINANCE_EMAIL, subject, body);
    }

    private String buildPaymentBody(BigDecimal amount) {
        return String.format("Pagamento recebido: %s", formatMoney(amount));
    }

    private String formatMoney(BigDecimal amount) {
        return String.format(BR_LOCALE, "R$ %,.2f", amount);
    }
}
