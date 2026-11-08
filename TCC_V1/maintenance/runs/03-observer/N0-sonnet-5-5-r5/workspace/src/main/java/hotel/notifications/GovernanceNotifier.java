package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;

/** Tells housekeeping which rooms to prepare and which ones were freed. */
public final class GovernanceNotifier implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public GovernanceNotifier(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation) {
        emailSender.send(
                address,
                "Reserva confirmada - quarto " + reservation.roomNumber(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }
        emailSender.send(
                address,
                "Reserva cancelada - quarto " + reservation.roomNumber(),
                "Datas liberadas: de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
