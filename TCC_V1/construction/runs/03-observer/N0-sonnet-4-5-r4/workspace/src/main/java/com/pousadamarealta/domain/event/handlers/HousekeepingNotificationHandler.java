package com.pousadamarealta.domain.event.handlers;

import com.pousadamarealta.domain.event.EventHandler;
import com.pousadamarealta.domain.event.ReservationCancelledEvent;
import com.pousadamarealta.domain.event.ReservationConfirmedEvent;
import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.service.MessageService;
import com.pousadamarealta.domain.util.DateFormatter;

public class HousekeepingNotificationHandler {
    private final MessageService messageService;

    public HousekeepingNotificationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public EventHandler<ReservationConfirmedEvent> onConfirmed() {
        return event -> {
            Reservation r = event.reservation();
            String body = String.format(
                "Entrada: %s | Saída: %s | %d hóspede%s",
                DateFormatter.format(r.getCheckIn()),
                DateFormatter.format(r.getCheckOut()),
                r.getGuests(),
                r.getGuests() == 1 ? "" : "s"
            );

            Message message = new Message(
                "EMAIL",
                "governanca@pousadamarealta.com.br",
                "Reserva confirmada - quarto " + r.getRoomNumber(),
                body
            );

            messageService.send(message);
        };
    }

    public EventHandler<ReservationCancelledEvent> onCancelled() {
        return event -> {
            if (!event.wasConfirmed()) {
                return;
            }

            Reservation r = event.reservation();
            String body = String.format(
                "Datas liberadas: %s a %s",
                DateFormatter.format(r.getCheckIn()),
                DateFormatter.format(r.getCheckOut())
            );

            Message message = new Message(
                "EMAIL",
                "governanca@pousadamarealta.com.br",
                "Reserva cancelada - quarto " + r.getRoomNumber(),
                body
            );

            messageService.send(message);
        };
    }
}
