package com.pousadamarealta.listeners;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.events.ReservationEventListener;
import com.pousadamarealta.services.MessageService;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.utils.Formatting;

public class GovernanceNotificationListener implements ReservationEventListener {
    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";
    private final MessageService messageService;

    public GovernanceNotificationListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %d", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s\nSaída: %s\nHóspedes: %d",
            Formatting.formatDate(reservation.getCheckIn()),
            Formatting.formatDate(reservation.getCheckOut()),
            reservation.getGuests()
        );

        messageService.sendMessage(MessageChannel.EMAIL, GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        String subject = String.format("Reserva cancelada - quarto %d", reservation.getRoomNumber());
        String body = String.format(
            "Entrada: %s\nSaída: %s",
            Formatting.formatDate(reservation.getCheckIn()),
            Formatting.formatDate(reservation.getCheckOut())
        );

        messageService.sendMessage(MessageChannel.EMAIL, GOVERNANCE_EMAIL, subject, body);
    }
}
