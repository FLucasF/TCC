package com.pousadamarealta.event;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.MessageService;

import java.time.format.DateTimeFormatter;

public class HousekeepingCancellationHandler implements EventHandler<ReservationCancelledEvent> {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MessageService messageService;

    public HousekeepingCancellationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(ReservationCancelledEvent event) {
        if (!event.wasConfirmed()) {
            return;
        }

        Reservation r = event.getReservation();

        String subject = String.format("Reserva cancelada - quarto %d", r.getRoomNumber());
        String body = String.format(
            "Datas liberadas:\nEntrada: %s\nSaída: %s",
            r.getCheckIn().format(DATE_FORMATTER),
            r.getCheckOut().format(DATE_FORMATTER)
        );

        messageService.send(MessageChannel.EMAIL, HOUSEKEEPING_EMAIL, subject, body);
    }
}
