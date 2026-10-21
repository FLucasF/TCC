package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationEvent;
import hotel.reservations.ReservationListener;

/** Tells housekeeping which rooms to get ready, and which ones were freed. */
public final class HousekeepingNotifier implements ReservationListener {

    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void on(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed confirmed -> confirmed(confirmed.reservation());
            case ReservationEvent.Cancelled cancelled -> {
                if (cancelled.wasConfirmed()) {
                    cancelled(cancelled.reservation());
                }
            }
        }
    }

    private void confirmed(Reservation reservation) {
        emailSender.send(
                ADDRESS,
                "Reserva confirmada - quarto " + reservation.roomNumber(),
                "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    private void cancelled(Reservation reservation) {
        emailSender.send(
                ADDRESS,
                "Reserva cancelada - quarto " + reservation.roomNumber(),
                "Datas liberadas: de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + "\n");
    }
}
