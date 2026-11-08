package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

public final class HousekeepingNotifier implements ReservationListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Reserva confirmada.\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        if (reservation.status() != ReservationStatus.CANCELLED) {
            return;
        }
        if (reservation.amountPaid().signum() == 0) {
            return;
        }
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Reserva cancelada.\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Datas livres:\n"
                + "  De: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "  Até: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
