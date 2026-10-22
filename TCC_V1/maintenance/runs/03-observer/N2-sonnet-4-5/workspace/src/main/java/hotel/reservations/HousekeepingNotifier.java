package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

public final class HousekeepingNotifier implements ReservationListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Reserva confirmada:\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Reserva cancelada:\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
