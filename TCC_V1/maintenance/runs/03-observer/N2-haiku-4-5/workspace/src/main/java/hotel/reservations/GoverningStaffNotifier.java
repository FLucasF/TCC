package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

public final class GoverningStaffNotifier implements ReservationListener {

    private final EmailSender emailSender;

    public GoverningStaffNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva confirmada - quarto " + reservation.roomNumber(),
                confirmationEmailBody(reservation));
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva cancelada - quarto " + reservation.roomNumber(),
                cancellationEmailBody(reservation));
    }

    private static String confirmationEmailBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn())
                + "\nSaída: " + TextFormat.date(reservation.checkOut())
                + "\nHóspedes: " + reservation.guests();
    }

    private static String cancellationEmailBody(Reservation reservation) {
        return "Entrada: " + TextFormat.date(reservation.checkIn())
                + "\nSaída: " + TextFormat.date(reservation.checkOut());
    }
}
