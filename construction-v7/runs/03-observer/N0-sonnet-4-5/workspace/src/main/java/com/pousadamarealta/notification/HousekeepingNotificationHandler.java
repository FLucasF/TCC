package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class HousekeepingNotificationHandler implements NotificationHandler {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String EMAIL = "governanca@pousadamarealta.com.br";
    private long messageIdCounter = 1;

    @Override
    public List<NotificationMessage> handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            return List.of(createConfirmationEmail(confirmed.reservation()));
        } else if (event instanceof ReservationCancelledEvent cancelled) {
            if (cancelled.previousStatus() == ReservationStatus.CONFIRMED) {
                return List.of(createCancellationEmail(cancelled.reservation()));
            }
        }
        return Collections.emptyList();
    }

    private NotificationMessage createConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
        String body = String.format(
            "Nova reserva confirmada.\nQuarto: %s\nEntrada: %s\nSaída: %s\nHóspedes: %d",
            reservation.getRoomNumber(),
            DATE_FORMAT.format(reservation.getCheckIn()),
            DATE_FORMAT.format(reservation.getCheckOut()),
            reservation.getGuests()
        );

        return new NotificationMessage(messageIdCounter++, "EMAIL", EMAIL, subject, body);
    }

    private NotificationMessage createCancellationEmail(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
        String body = String.format(
            "Reserva cancelada.\nQuarto: %s\nDatas liberadas: %s a %s",
            reservation.getRoomNumber(),
            DATE_FORMAT.format(reservation.getCheckIn()),
            DATE_FORMAT.format(reservation.getCheckOut())
        );

        return new NotificationMessage(messageIdCounter++, "EMAIL", EMAIL, subject, body);
    }

    public void setMessageIdCounter(long counter) {
        this.messageIdCounter = counter;
    }
}
