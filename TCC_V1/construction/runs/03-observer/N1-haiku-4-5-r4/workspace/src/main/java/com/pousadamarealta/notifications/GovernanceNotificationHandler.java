package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.utils.FormatterUtils;

public class GovernanceNotificationHandler implements NotificationHandler {

    @Override
    public void handleConfirmation(Reservation reservation, MessageRepository messageRepo) throws Exception {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber;
        String body = "Entrada: " + FormatterUtils.formatDate(reservation.checkIn) + "\n" +
            "Saída: " + FormatterUtils.formatDate(reservation.checkOut) + "\n" +
            "Hóspedes: " + reservation.guests;

        messageRepo.create("EMAIL", "governanca@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void handleCancellation(Reservation reservation, MessageRepository messageRepo) throws Exception {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber;
        String body = "Entrada: " + FormatterUtils.formatDate(reservation.checkIn) + "\n" +
            "Saída: " + FormatterUtils.formatDate(reservation.checkOut);

        messageRepo.create("EMAIL", "governanca@pousadamarealta.com.br", subject, body);
    }
}
