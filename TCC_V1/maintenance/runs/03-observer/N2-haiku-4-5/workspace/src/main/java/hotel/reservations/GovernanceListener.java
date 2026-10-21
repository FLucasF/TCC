package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import hotel.rooms.Rooms;

/** Notifies the housekeeping department of confirmations and cancellations. */
public final class GovernanceListener implements ReservationEventListener {

    private final Rooms rooms;
    private final EmailSender emailSender;

    public GovernanceListener(Rooms rooms, EmailSender emailSender) {
        this.rooms = rooms;
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        Room room = rooms.find(reservation.roomNumber()).orElseThrow();
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        Room room = rooms.find(reservation.roomNumber()).orElseThrow();
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }
}
