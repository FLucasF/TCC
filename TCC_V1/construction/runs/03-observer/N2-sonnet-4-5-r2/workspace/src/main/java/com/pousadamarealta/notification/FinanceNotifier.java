package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.util.Formatter;

import java.math.BigDecimal;

public class FinanceNotifier implements ReservationObserver {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final NotificationService notificationService;

    public FinanceNotifier(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());

        String body = String.format(
            "Pagamento recebido:%n%n" +
            "Reserva: %d%n" +
            "Valor: %s",
            reservation.getId(),
            Formatter.formatMoney(reservation.getAmountPaid())
        );

        notificationService.sendEmail(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }

        if (reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = String.format("Reembolso - reserva %d", reservation.getId());

        String body = String.format(
            "Reembolso a processar:%n%n" +
            "Reserva: %d%n" +
            "Valor: %s",
            reservation.getId(),
            Formatter.formatMoney(reservation.getRefund())
        );

        notificationService.sendEmail(FINANCE_EMAIL, subject, body);
    }
}
