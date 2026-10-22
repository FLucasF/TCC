package hotel.reservations.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

class HousekeepingNotifier implements ReservationEventNotifier {
    private final EmailSender emailSender;
    private final String housekeepingEmail;

    HousekeepingNotifier(EmailSender emailSender, String housekeepingEmail) {
        this.emailSender = emailSender;
        this.housekeepingEmail = housekeepingEmail;
    }

    @Override
    public void notifyConfirmation(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Quarto " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
        emailSender.send(housekeepingEmail, subject, body);
    }

    @Override
    public void notifyCancellation(Reservation reservation, Room room) {
        if (reservation.status() != ReservationStatus.CANCELLED) {
            return;
        }
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Quarto " + reservation.roomNumber() + "\n"
                + "Datas liberadas: " + TextFormat.date(reservation.checkIn()) + " a "
                + TextFormat.date(reservation.checkOut());
        emailSender.send(housekeepingEmail, subject, body);
    }
}
