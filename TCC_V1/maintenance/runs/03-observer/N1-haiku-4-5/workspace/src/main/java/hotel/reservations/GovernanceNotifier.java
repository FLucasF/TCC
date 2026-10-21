package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

public final class GovernanceNotifier implements ReservationNotifier {

    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public GovernanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }
}
