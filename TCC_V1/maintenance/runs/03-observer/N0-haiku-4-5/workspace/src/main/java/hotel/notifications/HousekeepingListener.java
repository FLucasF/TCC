package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Notifies housekeeping (governança) about confirmed and cancelled reservations. */
public final class HousekeepingListener implements ReservationEventListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn())
                + "\nSaída: " + TextFormat.date(reservation.checkOut())
                + "\nHóspedes: " + reservation.guests();
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn())
                + "\nSaída: " + TextFormat.date(reservation.checkOut());
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
