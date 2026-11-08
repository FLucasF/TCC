package com.pousadamarealta.event.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;

import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationListener implements
        EventListener<ReservationConfirmedEvent> {

    private final NotificationService notificationService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public HousekeepingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();

        String subject = "Reserva confirmada - quarto " + reservation.getRoomNumber();
        String body = String.format("Entrada: %s | Saída: %s | %d %s",
                reservation.getCheckIn().format(dateFormatter),
                reservation.getCheckOut().format(dateFormatter),
                reservation.getGuests(),
                reservation.getGuests() == 1 ? "hóspede" : "hóspedes");

        notificationService.sendEmail("governanca@pousadamarealta.com.br", subject, body);
    }
}
