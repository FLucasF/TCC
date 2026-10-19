package com.pousadamarealta.domain.listener;

import com.pousadamarealta.domain.event.Event;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class HousekeepingNotificationListener implements Consumer<Event> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final MessageService messageService;

    public HousekeepingNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void accept(Event event) {
        if (event instanceof ReservationConfirmedEvent confirmedEvent) {
            sendConfirmationNotification(confirmedEvent.reservation());
        } else if (event instanceof ReservationCancelledEvent cancelledEvent) {
            if (cancelledEvent.wasConfirmed()) {
                sendCancellationNotification(cancelledEvent.reservation());
            }
        }
    }

    private void sendConfirmationNotification(Reservation reservation) {
        String body = String.format(
            "Entrada: %s%nSaída: %s%nHóspedes: %d",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests()
        );

        messageService.sendEmail(
            HOUSEKEEPING_EMAIL,
            String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber()),
            body
        );
    }

    private void sendCancellationNotification(Reservation reservation) {
        String body = String.format(
            "Entrada: %s%nSaída: %s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER)
        );

        messageService.sendEmail(
            HOUSEKEEPING_EMAIL,
            String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber()),
            body
        );
    }
}
