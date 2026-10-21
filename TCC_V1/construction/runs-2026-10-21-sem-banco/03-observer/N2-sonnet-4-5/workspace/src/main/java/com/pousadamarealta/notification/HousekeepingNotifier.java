package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class HousekeepingNotifier implements ReservationListener {
    private static final String EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageSender messageSender;

    public HousekeepingNotifier(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed confirmed ->
                notifyConfirmation(confirmed.reservation());
            case ReservationEvent.Cancelled cancelled when cancelled.wasConfirmed() ->
                notifyCancellation(cancelled.reservation());
            default -> {}
        }
    }

    private void notifyConfirmation(Reservation reservation) {
        String body = String.format(
            "Entrada: %s\nSaída: %s\nHóspedes: %d",
            formatDate(reservation.checkIn()),
            formatDate(reservation.checkOut()),
            reservation.guests()
        );

        messageSender.send(
            MessageChannel.EMAIL,
            EMAIL,
            String.format("Reserva confirmada - quarto %d", reservation.roomNumber()),
            body
        );
    }

    private void notifyCancellation(Reservation reservation) {
        String body = String.format(
            "Entrada: %s\nSaída: %s",
            formatDate(reservation.checkIn()),
            formatDate(reservation.checkOut())
        );

        messageSender.send(
            MessageChannel.EMAIL,
            EMAIL,
            String.format("Reserva cancelada - quarto %d", reservation.roomNumber()),
            body
        );
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
