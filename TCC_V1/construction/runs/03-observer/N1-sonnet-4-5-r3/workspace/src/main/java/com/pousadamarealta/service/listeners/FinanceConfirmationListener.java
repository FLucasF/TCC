package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.DocumentFormatter;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmed;
import com.pousadamarealta.service.NotificationService;

public class FinanceConfirmationListener implements EventListener<ReservationConfirmed> {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final NotificationService notificationService;

    public FinanceConfirmationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmed event) {
        Reservation reservation = event.reservation();

        String subject = "Pagamento recebido - reserva " + reservation.getId();
        String body = "Valor recebido: " + DocumentFormatter.formatMoney(reservation.getAmountPaid());

        notificationService.sendEmail(FINANCE_EMAIL, subject, body);
    }
}
