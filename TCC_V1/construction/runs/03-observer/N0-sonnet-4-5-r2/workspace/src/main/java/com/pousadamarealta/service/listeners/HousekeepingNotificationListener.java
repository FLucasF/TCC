package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationCancelledEvent;
import com.pousadamarealta.domain.events.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public class HousekeepingNotificationListener implements
    EventListener<ReservationConfirmedEvent> {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageService messageService;

    public HousekeepingNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();

        String subject = "Reserva confirmada - quarto " + r.getRoomNumber();
        String body = String.format(
            "Uma reserva foi confirmada:\n\n" +
            "Quarto: %s\n" +
            "Entrada: %s\n" +
            "Saída: %s\n" +
            "Hóspedes: %d",
            r.getRoomNumber(),
            Formatter.formatDate(r.getCheckIn()),
            Formatter.formatDate(r.getCheckOut()),
            r.getGuests()
        );

        messageService.sendEmail(HOUSEKEEPING_EMAIL, subject, body);
    }
}
