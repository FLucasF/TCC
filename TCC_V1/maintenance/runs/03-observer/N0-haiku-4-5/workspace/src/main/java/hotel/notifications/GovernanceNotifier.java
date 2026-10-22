package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

/** Notifies the housekeeping team about reservation events. */
public final class GovernanceNotifier implements ReservationNotifier {

    private final EmailSender emailSender;

    public GovernanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva confirmada - quarto " + room.number(),
                buildConfirmationBody(reservation));
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva cancelada - quarto " + room.number(),
                buildCancellationBody(reservation));
    }

    private static String buildConfirmationBody(Reservation reservation) {
        return "A reserva nº " + reservation.id() + " foi confirmada.\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
    }

    private static String buildCancellationBody(Reservation reservation) {
        return "A reserva nº " + reservation.id() + " foi cancelada.\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
    }
}
