package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Tells housekeeping (governança) when a room has to be prepared or is freed. */
public final class HousekeepingNotifier implements ReservationListener {

    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                ADDRESS,
                "Reserva confirmada - quarto " + room.number(),
                "Reserva " + reservation.id() + " confirmada para o quarto " + room.number() + ".\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onConfirmedReservationCancelled(Reservation reservation, Room room) {
        emailSender.send(
                ADDRESS,
                "Reserva cancelada - quarto " + room.number(),
                "A reserva " + reservation.id() + " foi cancelada e o quarto " + room.number()
                        + " ficou livre nas datas:\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
