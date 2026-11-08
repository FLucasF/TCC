package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

public final class GovernanceNotification implements ReservationNotification {

    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public GovernanceNotification(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = formatConfirmationBody(reservation);
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = formatCancellationBody(reservation);
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    private static String formatConfirmationBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
    }

    private static String formatCancellationBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
    }
}
