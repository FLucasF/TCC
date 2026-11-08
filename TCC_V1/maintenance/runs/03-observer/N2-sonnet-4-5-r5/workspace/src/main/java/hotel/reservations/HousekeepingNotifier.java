package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

/** Notifies housekeeping when a reservation is confirmed or cancelled. */
public final class HousekeepingNotifier implements ReservationListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Nova reserva confirmada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Reserva cancelada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Datas livres: " + TextFormat.date(reservation.checkIn())
                + " a " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
