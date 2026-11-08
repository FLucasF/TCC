package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;

/** Tells housekeeping when a room has to be prepared and when its nights are free again. */
public final class HousekeepingEmail implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public HousekeepingEmail(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void confirmed(Reservation reservation) {
        emailSender.send(
                address,
                "Reserva confirmada - quarto " + reservation.roomNumber(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void cancelled(Reservation reservation, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED) {
            return;
        }
        emailSender.send(
                address,
                "Reserva cancelada - quarto " + reservation.roomNumber(),
                "Datas liberadas: de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
