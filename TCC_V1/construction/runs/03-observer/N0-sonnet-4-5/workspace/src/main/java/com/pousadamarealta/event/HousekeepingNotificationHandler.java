package com.pousadamarealta.event;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.MessageService;

import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationHandler implements
        EventHandler<ReservationConfirmedEvent> {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MessageService messageService;

    public HousekeepingNotificationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.getReservation();

        String subject = String.format("Reserva confirmada - quarto %d", r.getRoomNumber());
        String body = String.format(
            "Entrada: %s\nSaída: %s\nHóspedes: %d",
            r.getCheckIn().format(DATE_FORMATTER),
            r.getCheckOut().format(DATE_FORMATTER),
            r.getGuests()
        );

        messageService.send(MessageChannel.EMAIL, HOUSEKEEPING_EMAIL, subject, body);
    }
}
