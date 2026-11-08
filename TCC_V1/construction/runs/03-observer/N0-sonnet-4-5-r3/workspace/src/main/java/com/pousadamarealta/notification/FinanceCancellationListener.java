package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.util.FormatUtil;

import java.math.BigDecimal;

public class FinanceCancellationListener implements EventListener<ReservationCancelledEvent> {

    private final NotificationService notificationService;
    private final String financeEmail = "financeiro@pousadamarealta.com.br";

    public FinanceCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (event.previousStatus() != ReservationStatus.CONFIRMED) {
            return;
        }

        Reservation reservation = event.reservation();

        if (reservation.getRefund() == null || reservation.getRefund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String body = String.format("""
            Valor a devolver: %s""",
            FormatUtil.formatMoney(reservation.getRefund())
        );

        notificationService.sendEmail(
            financeEmail,
            String.format("Reembolso - reserva %d", reservation.getId()),
            body
        );
    }
}
