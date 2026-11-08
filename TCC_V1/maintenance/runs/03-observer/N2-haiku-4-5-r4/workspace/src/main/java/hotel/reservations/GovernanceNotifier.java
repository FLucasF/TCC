package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

public final class GovernanceNotifier implements ReservationEventListener {

    private final EmailSender emailSender;

    public GovernanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }
}
