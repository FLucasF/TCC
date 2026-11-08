package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

public final class GovernanceListener implements ReservationEventListener {

    private final EmailSender emailSender;

    public GovernanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Entrada (liberada): " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída (liberada): " + TextFormat.date(reservation.checkOut());
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }
}
