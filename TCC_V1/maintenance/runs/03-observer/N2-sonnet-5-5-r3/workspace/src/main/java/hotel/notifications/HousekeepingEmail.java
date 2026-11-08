package hotel.notifications;

import static hotel.documents.TextFormat.date;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

/** Tells housekeeping when a room has to be prepared, and when it was freed. */
public final class HousekeepingEmail implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public HousekeepingEmail(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Reserva confirmada - quarto " + room.number(),
                "Entrada: " + date(reservation.checkIn()) + "\n"
                        + "Saída: " + date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, Room room, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED) {
            return;
        }
        emailSender.send(
                address,
                "Reserva cancelada - quarto " + room.number(),
                "Datas que ficaram livres: de " + date(reservation.checkIn())
                        + " a " + date(reservation.checkOut()) + "\n");
    }
}
