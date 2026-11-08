package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

/** Notifies the housekeeping department of confirmed and cancelled reservations. */
public final class GovernanceListener implements ReservationEventListener {

    private static final String GOVERNANCE_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public GovernanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = buildConfirmationBody(event);
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = buildCancellationBody(event);
        emailSender.send(GOVERNANCE_EMAIL, subject, body);
    }

    private String buildConfirmationBody(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests();
    }

    private String buildCancellationBody(ReservationCancelledEvent event) {
        Reservation reservation = event.reservation();
        return "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut());
    }
}
