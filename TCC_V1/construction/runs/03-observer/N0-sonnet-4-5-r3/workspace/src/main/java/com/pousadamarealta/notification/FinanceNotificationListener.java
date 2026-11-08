package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.util.FormatUtil;

import java.math.BigDecimal;

public class FinanceNotificationListener implements EventListener<ReservationConfirmedEvent> {

    private final NotificationService notificationService;
    private final String financeEmail = "financeiro@pousadamarealta.com.br";

    public FinanceNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();

        String body = String.format("""
            Valor pago: %s""",
            FormatUtil.formatMoney(reservation.getAmountPaid())
        );

        notificationService.sendEmail(
            financeEmail,
            String.format("Pagamento recebido - reserva %d", reservation.getId()),
            body
        );
    }
}
