package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import hotel.rooms.Rooms;

public final class GovernanceNotifier implements ReservationNotifier {

    static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

    private final Rooms rooms;
    private final EmailSender emailSender;

    public GovernanceNotifier(Rooms rooms, EmailSender emailSender) {
        this.rooms = rooms;
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        Room room = rooms.find(reservation.roomNumber()).orElseThrow();
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = confirmationEmailBody(reservation);
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        if (reservation.status() != ReservationStatus.CANCELLED) {
            return;
        }
        if (reservation.amountPaid().signum() <= 0) {
            return;
        }
        Room room = rooms.find(reservation.roomNumber()).orElseThrow();
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = cancellationEmailBody(reservation);
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    private static String confirmationEmailBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn())
                + " | Saída: " + TextFormat.date(reservation.checkOut())
                + " | " + reservation.guests() + " hóspede(s)";
    }

    private static String cancellationEmailBody(Reservation reservation) {
        return "Datas liberadas:\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
    }
}
