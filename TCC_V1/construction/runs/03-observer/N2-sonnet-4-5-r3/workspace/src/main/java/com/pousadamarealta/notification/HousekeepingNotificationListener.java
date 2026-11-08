package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.util.Formatter;

public final class HousekeepingNotificationListener implements ReservationEventListener {
    private final MessageService messageService;

    public HousekeepingNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, RoomType roomType) {
        String body = String.format(
            "Entrada: %s | Saída: %s | %d %s",
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut()),
            reservation.guests(),
            reservation.guests() == 1 ? "hóspede" : "hóspedes"
        );

        messageService.send(
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            String.format("Reserva confirmada - quarto %d", reservation.roomNumber()),
            body
        );
    }

    @Override
    public void onReservationCancelled(Reservation reservation, RoomType roomType, boolean wasPreviouslyConfirmed) {
        if (!wasPreviouslyConfirmed) {
            return;
        }

        String body = String.format(
            "%s a %s",
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut())
        );

        messageService.send(
            MessageChannel.EMAIL,
            "governanca@pousadamarealta.com.br",
            String.format("Reserva cancelada - quarto %d", reservation.roomNumber()),
            body
        );
    }
}
