package com.pousada.marealta.notification;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.util.FormatHelper;

public class FinanceNotificationHandler implements NotificationHandler {
    private final MessageService messageService;
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    public FinanceNotificationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handleConfirmation(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id;
        String body = String.format("Valor: %s", FormatHelper.formatCurrency(reservation.total));

        messageService.sendMessage("EMAIL", FINANCE_EMAIL, subject, body);
    }

    @Override
    public void handleCancellation(Reservation reservation) {
        if (reservation.refund != null && reservation.refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id;
            String body = String.format("Valor: %s", FormatHelper.formatCurrency(reservation.refund));

            messageService.sendMessage("EMAIL", FINANCE_EMAIL, subject, body);
        }
    }
}
