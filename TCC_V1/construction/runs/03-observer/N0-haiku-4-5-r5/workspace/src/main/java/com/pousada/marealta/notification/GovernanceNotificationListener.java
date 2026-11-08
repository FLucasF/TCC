package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.domain.Message.Channel;
import com.pousada.marealta.util.DateFormatter;

import java.sql.SQLException;

public class GovernanceNotificationListener implements NotificationListener {
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageRepository messageRepository;

    public GovernanceNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        try {
            String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
            String body = String.format("""
                Entrada: %s
                Saída: %s
                Hóspedes: %d
                """,
                DateFormatter.format(reservation.getCheckIn()),
                DateFormatter.format(reservation.getCheckOut()),
                reservation.getGuests()
            );
            messageRepository.save(Channel.EMAIL, GOVERNANCE_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send governance notification", e);
        }
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED &&
            reservation.getStatus() != Reservation.ReservationStatus.CHECKED_IN &&
            reservation.getStatus() != Reservation.ReservationStatus.CHECKED_OUT) {
            return;
        }

        try {
            String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
            String body = String.format("""
                Entrada: %s
                Saída: %s
                """,
                DateFormatter.format(reservation.getCheckIn()),
                DateFormatter.format(reservation.getCheckOut())
            );
            messageRepository.save(Channel.EMAIL, GOVERNANCE_EMAIL, subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send governance cancellation notification", e);
        }
    }
}
