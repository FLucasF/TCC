package com.pousada.marealta.notification.notifiers;

import com.pousada.marealta.notification.ReservationEvent;
import com.pousada.marealta.notification.ReservationNotifier;
import com.pousada.marealta.db.MessageRepository;
import com.pousada.marealta.enums.MessageChannel;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.util.DocumentGenerator;

public class GuestNotifier implements ReservationNotifier {
    @Override
    public void handleEvent(ReservationEvent event) throws Exception {
        if (event.getType() == ReservationEvent.Type.CONFIRMED) {
            Reservation reservation = event.getReservation();

            String subject = "Reserva confirmada - Pousada Maré Alta";
            String body = DocumentGenerator.generateConfirmationEmail(reservation);

            MessageRepository.create(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
        }
    }
}
