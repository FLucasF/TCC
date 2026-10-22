package com.pousadamarealta.service.event.listeners;

import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.events.ReservationEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.service.event.EventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationListener implements EventListener {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MessageRepository messageRepository;

    public HousekeepingNotificationListener(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void handle(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent) {
            notifyConfirmation((ReservationConfirmedEvent) event);
        } else if (event instanceof ReservationCancelledEvent cancelledEvent) {
            if (cancelledEvent.wasConfirmed()) {
                notifyCancellation(cancelledEvent);
            }
        }
    }

    private void notifyConfirmation(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();

        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s\nSaída: %s\nHóspedes: %d",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            reservation.getGuests()
        );

        Message message = Message.email(HOUSEKEEPING_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private void notifyCancellation(ReservationCancelledEvent event) {
        Reservation reservation = event.getReservation();

        String subject = String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s\nSaída: %s",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut())
        );

        Message message = Message.email(HOUSEKEEPING_EMAIL, subject, body);
        messageRepository.save(message);
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
}
