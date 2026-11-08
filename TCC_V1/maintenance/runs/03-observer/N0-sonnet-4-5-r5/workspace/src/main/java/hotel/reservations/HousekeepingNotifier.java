package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

public final class HousekeepingNotifier implements ReservationEventListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, int roomNumber) {
        String subject = "Reserva confirmada - quarto " + roomNumber;
        String body = "Uma reserva foi confirmada.\n\n"
                + "Quarto: " + roomNumber + "\n"
                + "Check-in: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Check-out: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, int roomNumber, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }
        String subject = "Reserva cancelada - quarto " + roomNumber;
        String body = "Uma reserva foi cancelada.\n\n"
                + "Quarto: " + roomNumber + "\n"
                + "Datas que ficaram livres:\n"
                + "  Check-in: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "  Check-out: " + TextFormat.date(reservation.checkOut()) + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
