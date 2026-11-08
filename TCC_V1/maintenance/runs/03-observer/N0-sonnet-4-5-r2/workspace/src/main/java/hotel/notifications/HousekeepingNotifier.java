package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;

/** Notifies housekeeping when reservations are confirmed or cancelled. */
public final class HousekeepingNotifier implements ReservationEventListener {

    private static final String EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        switch (event) {
            case ReservationConfirmed confirmed -> notifyConfirmation(confirmed.reservation());
            case ReservationCancelled cancelled -> {
                if (cancelled.wasConfirmed()) {
                    notifyCancellation(cancelled.reservation());
                }
            }
        }
    }

    private void notifyConfirmation(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Uma reserva foi confirmada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Check-in: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Check-out: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    private void notifyCancellation(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Uma reserva foi cancelada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Datas que ficaram livres:\n"
                + "  De: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "  Até: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }
}
