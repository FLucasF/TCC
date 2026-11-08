package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

final class GovernanceCancellationListener implements ReservationListener {

    private final EmailSender emailSender;

    GovernanceCancellationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Quarto " + reservation.roomNumber() + " liberado\n\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }
}
