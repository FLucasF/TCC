package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Rooms;

public final class GovernanceListener implements ReservationEventListener {

    private final EmailSender emailSender;
    private final Rooms rooms;

    public GovernanceListener(EmailSender emailSender, Rooms rooms) {
        this.emailSender = emailSender;
        this.rooms = rooms;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String roomNumber = String.valueOf(reservation.roomNumber());
        String subject = "Reserva confirmada - quarto " + roomNumber;
        String body = formatConfirmationBody(reservation);
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        String roomNumber = String.valueOf(reservation.roomNumber());
        String subject = "Reserva cancelada - quarto " + roomNumber;
        String body = formatCancellationBody(reservation);
        emailSender.send("governanca@pousadamarealta.com.br", subject, body);
    }

    private String formatConfirmationBody(Reservation reservation) {
        return "Quarto " + reservation.roomNumber() + " foi confirmado.\n\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
    }

    private String formatCancellationBody(Reservation reservation) {
        return "Quarto " + reservation.roomNumber() + " foi liberado.\n\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n";
    }
}
