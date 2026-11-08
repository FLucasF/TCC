package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.utils.FormatterUtils;

import java.math.BigDecimal;

public class FinanceNotificationHandler implements NotificationHandler {

    @Override
    public void handleConfirmation(Reservation reservation, MessageRepository messageRepo) throws Exception {
        String subject = "Pagamento recebido - reserva " + reservation.id;
        String body = "Valor: " + FormatterUtils.formatCurrency(reservation.total);

        messageRepo.create("EMAIL", "financeiro@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void handleCancellation(Reservation reservation, MessageRepository messageRepo) throws Exception {
        if (reservation.refund != null && reservation.refund.compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id;
            String body = "Valor: " + FormatterUtils.formatCurrency(reservation.refund);

            messageRepo.create("EMAIL", "financeiro@pousadamarealta.com.br", subject, body);
        }
    }
}
