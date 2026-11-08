package com.pousadamarealta.application.notification;

import com.pousadamarealta.domain.event.EventListener;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.model.Reservation;
import java.time.format.DateTimeFormatter;

public class HousekeepingCancellationListener implements EventListener<ReservationCancelledEvent> {

    private final NotificationService notificationService;
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public HousekeepingCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.reservation();
        String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
        String body = buildCancellationBody(reservation);
        notificationService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }

    private String buildCancellationBody(Reservation reservation) {
        return String.format("Reserva cancelada. Datas livres:\nEntrada: %s\nSaída: %s",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER));
    }
}
