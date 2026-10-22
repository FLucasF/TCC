package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationEventListener;
import com.pousadamarealta.domain.MessageChannel;
import java.text.DecimalFormat;

public class FinanceNotificationListener implements ReservationEventListener {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("0.00");

    @Override
    public void onConfirmed(Reservation reservation) {
        try {
            String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
            String amount = String.format("R$ %s", formatMoney(reservation.getAmountPaid().toDouble()));
            String body = String.format("Valor: %s", amount);
            MessageService.getInstance().sendMessage(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send finance notification", e);
        }
    }

    @Override
    public void onCancelled(Reservation reservation) {
        try {
            if (reservation.getRefund() != null && reservation.getRefund().toDouble() > 0) {
                String subject = String.format("Reembolso - reserva %d", reservation.getId());
                String amount = String.format("R$ %s", formatMoney(reservation.getRefund().toDouble()));
                String body = String.format("Valor: %s", amount);
                MessageService.getInstance().sendMessage(MessageChannel.EMAIL, FINANCE_EMAIL, subject, body);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send finance notification", e);
        }
    }

    private String formatMoney(double amount) {
        return MONEY_FORMATTER.format(amount).replace(',', '.');
    }
}
