package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public class HousekeepingCancellationListener implements
    EventListener<ReservationCancelledEvent> {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageService messageService;

    public HousekeepingCancellationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation r = event.getReservation();

        String subject = "Reserva cancelada - quarto " + r.getRoomNumber();
        String body = String.format(
            "Uma reserva foi cancelada:\n\n" +
            "Quarto: %s\n" +
            "Datas liberadas: %s a %s",
            r.getRoomNumber(),
            Formatter.formatDate(r.getCheckIn()),
            Formatter.formatDate(r.getCheckOut())
        );

        messageService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }
}
