package com.marealta.application.handler;

import com.marealta.domain.event.ReservationCancelledEvent;
import com.marealta.domain.model.Reservation;
import com.marealta.domain.model.ReservationStatus;
import com.marealta.infrastructure.messaging.EmailService;
import com.marealta.infrastructure.notification.EventHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HousekeepingCancellationHandler implements EventHandler<ReservationCancelledEvent> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailService emailService;

    public HousekeepingCancellationHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (event.previousStatus() != ReservationStatus.CONFIRMED) {
            return;
        }

        Reservation reservation = event.reservation();
        String subject = String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Uma reserva foi cancelada.\n\nQuarto: %s\nDatas liberadas:\nEntrada: %s\nSaída: %s",
            reservation.getRoomNumber(),
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut())
        );

        emailService.send(EMAIL, subject, body);
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
