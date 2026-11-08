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
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Uma reserva foi confirmada.\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, boolean wasPaid) {
        if (!wasPaid) {
            return;
        }
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Uma reserva foi cancelada.\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Datas que ficaram livres:\n"
                + "  Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "  Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
