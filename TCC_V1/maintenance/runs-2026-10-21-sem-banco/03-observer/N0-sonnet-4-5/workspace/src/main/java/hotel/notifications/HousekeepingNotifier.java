package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationEventListener;
import hotel.rooms.Room;

/** Notifies housekeeping about confirmed and cancelled reservations. */
public final class HousekeepingNotifier implements ReservationEventListener {

    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Nova reserva confirmada:\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Reserva cancelada:\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Datas livres: " + TextFormat.date(reservation.checkIn())
                + " a " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }
}
