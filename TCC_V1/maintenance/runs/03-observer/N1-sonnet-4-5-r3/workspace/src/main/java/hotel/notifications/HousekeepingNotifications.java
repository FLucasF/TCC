package hotel.notifications;

import static hotel.documents.TextFormat.date;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Notifications for the housekeeping team: which rooms to prepare and which ones are freed up. */
public final class HousekeepingNotifications implements DepartmentNotifications {

    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = "Reserva confirmada.\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Entrada: " + date(reservation.checkIn()) + "\n"
                + "Saída: " + date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = "Reserva cancelada. As datas ficaram livres.\n\n"
                + "Quarto: " + room.number() + "\n"
                + "Período livre: " + date(reservation.checkIn()) + " a " + date(reservation.checkOut()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }
}
