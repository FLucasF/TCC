package com.pousadamarealta.notification;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.event.EventListener;
import com.pousadamarealta.event.ReservationCancelledEvent;
import com.pousadamarealta.event.ReservationConfirmedEvent;
import com.pousadamarealta.service.NotificationService;
import com.pousadamarealta.util.FormatUtil;

public class HousekeepingNotificationListener implements
        EventListener<ReservationConfirmedEvent> {

    private final NotificationService notificationService;
    private final String housekeepingEmail = "governanca@pousadamarealta.com.br";

    public HousekeepingNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();

        String body = String.format("""
            Entrada: %s | Saída: %s
            Hóspedes: %d""",
            FormatUtil.formatDate(reservation.getCheckIn()),
            FormatUtil.formatDate(reservation.getCheckOut()),
            reservation.getGuests()
        );

        notificationService.sendEmail(
            housekeepingEmail,
            String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber()),
            body
        );
    }
}
