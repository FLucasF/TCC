package com.pousadamarealta.event.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.service.NotificationService;

import java.time.format.DateTimeFormatter;

public class HousekeepingCancellationListener implements EventListener<ReservationCancelledEvent> {

    private final NotificationService notificationService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public HousekeepingCancellationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation reservation = event.getReservation();

        String subject = "Reserva cancelada - quarto " + reservation.getRoomNumber();
        String body = String.format("Entrada: %s | Saída: %s",
                reservation.getCheckIn().format(dateFormatter),
                reservation.getCheckOut().format(dateFormatter));

        notificationService.sendEmail("governanca@pousadamarealta.com.br", subject, body);
    }
}
