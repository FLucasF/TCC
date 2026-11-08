package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Housekeeping learns when a room has to be prepared and when it is freed. */
final class HousekeepingNotice implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    HousekeepingNotice(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Reserva confirmada - quarto " + room.number(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void cancelled(Reservation reservation, Room room, boolean wasConfirmed) {
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
