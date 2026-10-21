package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Tells housekeeping when a room must be prepared and when its dates are freed. */
public final class HousekeepingNotice implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public HousekeepingNotice(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Reserva confirmada - quarto " + room.number(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, Room room, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }
        emailSender.send(
                address,
                "Reserva cancelada - quarto " + room.number(),
                "Datas liberadas: de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
