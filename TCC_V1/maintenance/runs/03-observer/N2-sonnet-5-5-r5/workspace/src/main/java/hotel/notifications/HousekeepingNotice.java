package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

/** Tells housekeeping which room to prepare, and when a confirmed room is freed. */
public final class HousekeepingNotice implements ReservationListener {

    static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotice(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        emailSender.send(
                ADDRESS,
                "Reserva confirmada - quarto " + room.number(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void cancelled(Reservation reservation, Room room, ReservationStatus statusBefore) {
        if (statusBefore != ReservationStatus.CONFIRMED) {
            return;
        }
        emailSender.send(
                ADDRESS,
                "Reserva cancelada - quarto " + room.number(),
                "Datas liberadas: de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
