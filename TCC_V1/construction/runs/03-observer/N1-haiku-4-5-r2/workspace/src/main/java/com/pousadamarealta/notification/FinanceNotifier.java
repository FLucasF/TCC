package com.pousadamarealta.notification;

import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.util.DateFormatter;
import java.sql.SQLException;

public class FinanceNotifier implements ReservationEventListener {
    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    @Override
    public void onConfirmed(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.id);
        String body = String.format("Valor: %s", DateFormatter.formatCurrency(reservation.amountPaid));
        sendMessage(subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation) {
        if (reservation.refund != null && reservation.refund.signum() > 0) {
            String subject = String.format("Reembolso - reserva %d", reservation.id);
            String body = String.format("Valor: %s", DateFormatter.formatCurrency(reservation.refund));
            sendMessage(subject, body);
        }
    }

    private void sendMessage(String subject, String body) {
        try {
            MessageRepository.insert("EMAIL", EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }
}
