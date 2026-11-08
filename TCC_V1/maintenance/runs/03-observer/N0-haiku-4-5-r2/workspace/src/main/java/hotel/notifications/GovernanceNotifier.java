package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

public final class GovernanceNotifier implements Notifier {

    private final String email;
    private final EmailSender emailSender;

    public GovernanceNotifier(String email, EmailSender emailSender) {
        this.email = email;
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = formatConfirmationBody(reservation);
        emailSender.send(email, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = formatCancellationBody(reservation);
        emailSender.send(email, subject, body);
    }

    private static String formatConfirmationBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
    }

    private static String formatCancellationBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
    }
}
