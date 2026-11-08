package hotel.notifications;

import static hotel.documents.TextFormat.date;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;

/** Tells housekeeping which rooms to prepare, and which ones were freed. */
public final class GovernanceNotifier implements ReservationListener {

    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public GovernanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Confirmed event) {
        Reservation reservation = event.reservation();
        emailSender.send(
                ADDRESS,
                "Reserva confirmada - quarto " + event.room().number(),
                "Quarto " + event.room().number() + " reservado.\n"
                        + "Entrada: " + date(reservation.checkIn()) + "\n"
                        + "Saída: " + date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onCancelled(Cancelled event) {
        if (!event.wasConfirmed()) {
            return;
        }
        Reservation reservation = event.reservation();
        emailSender.send(
                ADDRESS,
                "Reserva cancelada - quarto " + event.room().number(),
                "A reserva do quarto " + event.room().number() + " foi cancelada.\n"
                        + "Datas liberadas: de " + date(reservation.checkIn())
                        + " a " + date(reservation.checkOut()) + "\n");
    }
}
