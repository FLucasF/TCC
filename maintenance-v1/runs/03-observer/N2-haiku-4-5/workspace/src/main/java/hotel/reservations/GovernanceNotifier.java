package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

/** Notifies governance when a reservation is confirmed or cancelled. */
public final class GovernanceNotifier implements ReservationEventHandler {

    private static final String EMAIL = "governanca@pousadamarealta.com.br";
    private final EmailSender emailSender;

    public GovernanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Quarto: " + room.number() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        if (reservation.status() != ReservationStatus.CANCELLED) {
            return;
        }
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Quarto: " + room.number() + "\n"
                + "Datas liberadas: " + TextFormat.date(reservation.checkIn())
                + " a " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }
}
