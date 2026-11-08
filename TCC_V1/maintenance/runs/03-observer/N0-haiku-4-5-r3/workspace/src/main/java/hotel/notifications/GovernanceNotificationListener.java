package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

public final class GovernanceNotificationListener implements ReservationNotificationListener {

    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";
    private final EmailSender emailSender;

    public GovernanceNotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Reserva confirmada - quarto " + room.number();
        String body = String.format(
                "Quarto %d confirmado.%n"
                        + "Entrada: %s%n"
                        + "Saída: %s%n"
                        + "Hóspedes: %d",
                room.number(),
                TextFormat.date(reservation.checkIn()),
                TextFormat.date(reservation.checkOut()),
                reservation.guests());
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        String subject = "Reserva cancelada - quarto " + room.number();
        String body = String.format(
                "Quarto %d foi liberado.%n"
                        + "Entrada (que não vai ocorrer): %s%n"
                        + "Saída (que não vai ocorrer): %s",
                room.number(),
                TextFormat.date(reservation.checkIn()),
                TextFormat.date(reservation.checkOut()));
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }
}
