package com.pousada.marealta.notification;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.service.MessageService;
import com.pousada.marealta.util.FormatHelper;

public class GoveranceNotificationHandler implements NotificationHandler {
    private final MessageService messageService;
    private static final String GOVERANCE_EMAIL = "governanca@pousadamarealta.com.br";

    public GoveranceNotificationHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handleConfirmation(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber;
        String body = String.format(
            "Entrada: %s\nSaída: %s\nHóspedes: %d",
            FormatHelper.formatBrDate(reservation.checkIn),
            FormatHelper.formatBrDate(reservation.checkOut),
            reservation.guests
        );

        messageService.sendMessage("EMAIL", GOVERANCE_EMAIL, subject, body);
    }

    @Override
    public void handleCancellation(Reservation reservation) {
        if (reservation.status == com.pousada.marealta.model.ReservationStatus.CANCELLED) {
            String subject = "Reserva cancelada - quarto " + reservation.roomNumber;
            String body = String.format(
                "Entrada: %s\nSaída: %s",
                FormatHelper.formatBrDate(reservation.checkIn),
                FormatHelper.formatBrDate(reservation.checkOut)
            );

            messageService.sendMessage("EMAIL", GOVERANCE_EMAIL, subject, body);
        }
    }
}
