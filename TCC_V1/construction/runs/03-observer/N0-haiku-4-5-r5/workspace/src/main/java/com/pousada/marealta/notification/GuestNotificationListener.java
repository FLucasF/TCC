package com.pousada.marealta.notification;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.MessageRepository;
import com.pousada.marealta.domain.Message.Channel;
import com.pousada.marealta.util.DateFormatter;
import com.pousada.marealta.util.DocumentFormatter;

import java.sql.SQLException;

public class GuestNotificationListener implements NotificationListener {
    private final MessageRepository messageRepository;

    public GuestNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        try {
            String subject = "Reserva confirmada - Pousada Maré Alta";
            String body = buildConfirmationEmail(reservation);
            messageRepository.save(Channel.EMAIL, reservation.getGuestEmail(), subject, body);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to send guest confirmation email", e);
        }
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
    }

    private String buildConfirmationEmail(Reservation reservation) {
        String document = DocumentFormatter.format(reservation);
        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);
    }
}
