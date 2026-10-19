package com.pousadamarealta.observer;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.service.EmailService;

import java.time.format.DateTimeFormatter;

public class HousekeepingNotificationObserver implements ReservationObserver {
    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    public HousekeepingNotificationObserver(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        switch (event.getType()) {
            case CONFIRMED -> sendConfirmationNotification(event.getReservation());
            case CANCELLED -> {
                if (event.getPreviousState() != null &&
                    event.getPreviousState().getStatus() == ReservationStatus.CONFIRMED) {
                    sendCancellationNotification(event.getReservation());
                }
            }
        }
    }

    private void sendConfirmationNotification(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());
        String body = String.format("Entrada: %s | Saída: %s | %d hóspede%s",
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT),
                reservation.getGuests(),
                reservation.getGuests() == 1 ? "" : "s");
        emailService.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    private void sendCancellationNotification(Reservation reservation) {
        String subject = String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber());
        String body = String.format("Datas liberadas: %s a %s",
                reservation.getCheckIn().format(DATE_FORMAT),
                reservation.getCheckOut().format(DATE_FORMAT));
        emailService.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
