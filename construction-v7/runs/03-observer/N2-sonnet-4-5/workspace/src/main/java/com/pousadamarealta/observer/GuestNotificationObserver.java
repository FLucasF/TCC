package com.pousadamarealta.observer;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.EmailService;
import com.pousadamarealta.service.ReservationService;

public class GuestNotificationObserver implements ReservationObserver {
    private final EmailService emailService;
    private final ReservationService reservationService;

    public GuestNotificationObserver(EmailService emailService, ReservationService reservationService) {
        this.emailService = emailService;
        this.reservationService = reservationService;
    }

    @Override
    public void onReservationEvent(ReservationEvent event) {
        if (event.getType() == ReservationEventType.CONFIRMED) {
            sendConfirmationEmail(event.getReservation());
        }
    }

    private void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmailBody(reservation);
        emailService.send(reservation.getGuestEmail(), subject, body);
    }

    private String buildConfirmationEmailBody(Reservation reservation) {
        String roomType = reservationService.getRoom(reservation.getRoomNumber()).getType().name();
        String receipt = emailService.generateReceipt(reservation, roomType);
        return String.format("Olá, %s!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                "%s\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!",
                reservation.getGuestName(), receipt);
    }
}
