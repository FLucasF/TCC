package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

final class GovernanceConfirmationListener implements ReservationListener {

    private final EmailSender emailSender;

    GovernanceConfirmationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Quarto " + reservation.roomNumber() + " reservado\n\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
    }
}
