package com.pousadamarealta.domain.event.listeners;

import com.pousadamarealta.domain.event.EventPublisher;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;

import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationListener implements
        EventPublisher.ReservationConfirmedListener,
        EventPublisher.ReservationCancelledListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MessageService messageService;

    public HousekeepingNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();

        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s\n" +
            "Saída: %s\n" +
            "Hóspedes: %d",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests()
        );

        Message message = new Message("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
        messageService.send(message);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.getReservation();

        String subject = String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s\n" +
            "Saída: %s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER)
        );

        Message message = new Message("EMAIL", HOUSEKEEPING_EMAIL, subject, body);
        messageService.send(message);
    }
}
