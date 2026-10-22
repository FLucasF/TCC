package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationEventListener;
import com.pousadamarealta.domain.MessageChannel;
import java.time.format.DateTimeFormatter;

public class GovernanceNotificationListener implements ReservationEventListener {
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void onConfirmed(Reservation reservation) {
        try {
            String subject = String.format("Reserva confirmada - quarto %d", reservation.getRoomNumber());
            String checkIn = reservation.getCheckIn().format(DATE_FORMATTER);
            String checkOut = reservation.getCheckOut().format(DATE_FORMATTER);
            String body = String.format("Entrada: %s | Saída: %s | Hóspedes: %d",
                    checkIn, checkOut, reservation.getGuests());
            MessageService.getInstance().sendMessage(MessageChannel.EMAIL, GOVERNANCE_EMAIL, subject, body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send governance notification", e);
        }
    }

    @Override
    public void onCancelled(Reservation reservation) {
        try {
            if (reservation.getAmountPaid().toDouble() > 0) {
                String subject = String.format("Reserva cancelada - quarto %d", reservation.getRoomNumber());
                String checkIn = reservation.getCheckIn().format(DATE_FORMATTER);
                String checkOut = reservation.getCheckOut().format(DATE_FORMATTER);
                String body = String.format("Entrada: %s | Saída: %s", checkIn, checkOut);
                MessageService.getInstance().sendMessage(MessageChannel.EMAIL, GOVERNANCE_EMAIL, subject, body);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send governance notification", e);
        }
    }
}
