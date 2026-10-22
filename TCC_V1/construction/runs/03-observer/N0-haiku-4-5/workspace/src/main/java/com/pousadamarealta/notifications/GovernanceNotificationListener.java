package com.pousadamarealta.notifications;

import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.ReservationStatus;
import com.pousadamarealta.utils.DateParser;
import com.pousadamarealta.MessageService;
import java.math.BigDecimal;

public class GovernanceNotificationListener implements NotificationListener {
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber;
        String body = String.format(
            "Quarto %d\nEntrada: %s\nSaída: %s\n%d hóspede(s)",
            reservation.roomNumber,
            DateParser.format(reservation.checkIn),
            DateParser.format(reservation.checkOut),
            reservation.guests
        );

        MessageService.sendMessage("EMAIL", GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, BigDecimal refundAmount) {
        if (reservation.status != ReservationStatus.CONFIRMED &&
            reservation.status != ReservationStatus.CHECKED_IN &&
            reservation.status != ReservationStatus.CHECKED_OUT) {
            return;
        }

        String subject = "Reserva cancelada - quarto " + reservation.roomNumber;
        String body = String.format(
            "Quarto %d\nEntrada: %s\nSaída: %s",
            reservation.roomNumber,
            DateParser.format(reservation.checkIn),
            DateParser.format(reservation.checkOut)
        );

        MessageService.sendMessage("EMAIL", GOVERNANCE_EMAIL, subject, body);
    }
}
