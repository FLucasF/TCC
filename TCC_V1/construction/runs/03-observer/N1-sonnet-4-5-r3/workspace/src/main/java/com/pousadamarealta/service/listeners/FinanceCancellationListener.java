package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.DocumentFormatter;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationCancelled;
import com.pousadamarealta.service.NotificationService;

import java.math.BigDecimal;

public class FinanceCancellationListener implements EventListener<ReservationCancelled> {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final NotificationService notificationService;

    public FinanceCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelled event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.reservation();

        if (reservation.getRefund() == null || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String subject = "Reembolso - reserva " + reservation.getId();
        String body = "Valor a devolver: " + DocumentFormatter.formatMoney(reservation.getRefund());

        notificationService.sendEmail(FINANCE_EMAIL, subject, body);
    }
}
