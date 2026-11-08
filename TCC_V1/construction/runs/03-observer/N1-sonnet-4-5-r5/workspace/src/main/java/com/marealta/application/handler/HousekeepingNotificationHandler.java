package com.marealta.application.handler;

import com.marealta.domain.event.ReservationCancelledEvent;
import com.marealta.domain.event.ReservationConfirmedEvent;
import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.ReservationStatus;
import com.marealta.infrastructure.messaging.EmailService;
import com.marealta.infrastructure.notification.EventHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationHandler implements
    EventHandler<ReservationConfirmedEvent> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailService emailService;

    public HousekeepingNotificationHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Uma reserva foi confirmada.\n\nQuarto: %s\nEntrada: %s\nSaída: %s\nHóspedes: %d",
            reservation.getRoomNumber(),
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            reservation.getGuests()
        );

        emailService.send(EMAIL, subject, body);
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
