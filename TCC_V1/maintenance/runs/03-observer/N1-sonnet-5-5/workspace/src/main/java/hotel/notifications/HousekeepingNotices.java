package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;

/** Tells housekeeping when a room has to be prepared, and when it is free again. */
public final class HousekeepingNotices implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public HousekeepingNotices(EmailSender emailSender, String address) {
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
    public void cancelledAfterConfirmation(Reservation reservation) {
        emailSender.send(
                address,
                "Reserva cancelada - quarto " + reservation.roomNumber(),
                "Datas que ficaram livres: " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
