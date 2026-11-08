package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Tells housekeeping which rooms to prepare, and which ones were freed. */
public final class Governance implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public Governance(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Reserva confirmada - quarto " + room.number(),
                "Reserva " + reservation.id() + " confirmada para o quarto " + room.number() + ".\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onConfirmedReservationCancelled(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Reserva cancelada - quarto " + room.number(),
                "Reserva " + reservation.id() + " cancelada. O quarto " + room.number() + " ficou livre nas datas:\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
