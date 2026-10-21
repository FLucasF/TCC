package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class HousekeepingNotifier implements ReservationEventListener {

    private final EmailSender emailSender;
    private final String email;

    public HousekeepingNotifier(EmailSender emailSender, String email) {
        this.emailSender = emailSender;
        this.email = email;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Nova reserva confirmada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(email, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, BigDecimal refund) {
        if (reservation.amountPaid().signum() > 0) {
            String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
            String body = "Reserva cancelada:\n\n"
                    + "Quarto: " + reservation.roomNumber() + "\n"
                    + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                    + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
            emailSender.send(email, subject, body);
        }
    }
}
