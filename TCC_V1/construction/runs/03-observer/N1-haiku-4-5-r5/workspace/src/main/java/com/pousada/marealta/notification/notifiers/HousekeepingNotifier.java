package com.pousada.marealta.notification.notifiers;

import com.pousada.marealta.notification.ReservationEvent;
import com.pousada.marealta.notification.ReservationNotifier;
import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.enums.MessageChannel;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.util.Formatter;

public class HousekeepingNotifier implements ReservationNotifier {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    @Override
    public void handleEvent(ReservationEvent event) throws Exception {
        Reservation reservation = event.getReservation();

        if (event.getType() == ReservationEvent.Type.CONFIRMED) {
            String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
            String body = String.format(
                "Quarto: %d\nEntrada: %s\nSaída: %s\nHóspedes: %d",
                reservation.getRoomNumber(),
                Formatter.formatDate(reservation.getCheckIn()),
                Formatter.formatDate(reservation.getCheckOut()),
                reservation.getGuests()
            );

            MessageRepository.create(MessageChannel.EMAIL, HOUSEKEEPING_EMAIL, subject, body);
        } else if (event.getType() == ReservationEvent.Type.CANCELLED) {
            String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
            String body = String.format(
                "Quarto: %d\nEntrada: %s\nSaída: %s",
                reservation.getRoomNumber(),
                Formatter.formatDate(reservation.getCheckIn()),
                Formatter.formatDate(reservation.getCheckOut())
            );

            MessageRepository.create(MessageChannel.EMAIL, HOUSEKEEPING_EMAIL, subject, body);
        }
    }
}
