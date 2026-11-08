package com.pousadamarealta.application.notification;

import com.pousadamarealta.domain.event.EventListener;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationListener implements
        EventListener<ReservationConfirmedEvent> {

    private final NotificationService notificationService;
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public HousekeepingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
        String body = buildConfirmationBody(reservation);
        notificationService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }

    private String buildConfirmationBody(Reservation reservation) {
        return String.format("Reserva confirmada:\nEntrada: %s\nSaída: %s\nHóspedes: %d",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests());
    }
}
