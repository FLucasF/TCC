package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationEvent;
import hotel.reservations.ReservationListener;

/** Tells housekeeping when a room must be prepared and when it was freed. */
public final class GovernanceNotifier implements ReservationListener {

    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public GovernanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void on(ReservationEvent event) {
        Reservation reservation = event.reservation();
        int room = event.room().number();
        switch (event) {
            case ReservationEvent.Confirmed confirmed -> emailSender.send(
                    ADDRESS,
                    "Reserva confirmada - quarto " + room,
                    "Entrada: " + TextFormat.date(reservation.checkIn())
                            + "\nSaída: " + TextFormat.date(reservation.checkOut())
                            + "\nHóspedes: " + reservation.guests() + "\n");
            case ReservationEvent.CancelledAfterPayment cancelled -> emailSender.send(
                    ADDRESS,
                    "Reserva cancelada - quarto " + room,
                    "Datas que ficaram livres: " + TextFormat.date(reservation.checkIn())
                            + " a " + TextFormat.date(reservation.checkOut()) + "\n");
        }
    }
}
