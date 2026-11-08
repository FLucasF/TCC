package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.domain.Message.Channel;
import com.pousada.marealta.util.MoneyFormatter;

import java.sql.SQLException;

public class FinanceNotificationListener implements NotificationListener {
    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public FinanceNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        try {
            String subject = "Pagamento recebido - reserva " + reservation.getId();
            String body = String.format("Valor: %s", MoneyFormatter.format(reservation.getTotal()));
            messageRepository.save(Channel.EMAIL, FINANCE_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send finance notification", e);
        }
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        if (reservation.getRefund() == null || reservation.getRefund().signum() == 0) {
            return;
        }

        try {
            String subject = "Reembolso - reserva " + reservation.getId();
            String body = String.format("Valor: %s", MoneyFormatter.format(reservation.getRefund()));
            messageRepository.save(Channel.EMAIL, FINANCE_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send finance cancellation notification", e);
        }
    }
}
