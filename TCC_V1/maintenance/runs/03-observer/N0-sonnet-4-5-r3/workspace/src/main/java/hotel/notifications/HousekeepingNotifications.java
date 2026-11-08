package hotel.notifications;

import static hotel.documents.TextFormat.date;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Sends notifications to the housekeeping department about reservations. */
public final class HousekeepingNotifications implements ReservationEventListener {

    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Nova reserva confirmada:\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + date(reservation.checkIn()) + "\n"
                + "Saída: " + date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Reserva cancelada:\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Datas liberadas:\n"
                + "Entrada: " + date(reservation.checkIn()) + "\n"
                + "Saída: " + date(reservation.checkOut()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }
}
