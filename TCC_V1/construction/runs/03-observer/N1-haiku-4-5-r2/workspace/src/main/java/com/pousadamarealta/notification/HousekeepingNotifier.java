package com.pousadamarealta.notification;

import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.util.DateFormatter;
import java.sql.SQLException;

public class HousekeepingNotifier implements ReservationEventListener {
    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    @Override
    public void onConfirmed(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %d", reservation.roomNumber);
        String body = String.format(
            "Entrada: %s | Saída: %s | %d hóspede(s)",
            DateFormatter.format(reservation.checkIn),
            DateFormatter.format(reservation.checkOut),
            reservation.guests
        );
        sendMessage(subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation) {
        if (reservation.amountPaid.signum() > 0) {
            String subject = String.format("Reserva cancelada - quarto %d", reservation.roomNumber);
            String body = String.format(
                "Entrada: %s | Saída: %s",
                DateFormatter.format(reservation.checkIn),
                DateFormatter.format(reservation.checkOut)
            );
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
