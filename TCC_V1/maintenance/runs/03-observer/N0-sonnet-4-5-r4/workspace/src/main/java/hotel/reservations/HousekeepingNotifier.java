package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

/** Notifies housekeeping when rooms need to be prepared or freed up. */
public final class HousekeepingNotifier implements ReservationListener {

    private static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Reserva confirmada - quarto " + reservation.roomNumber();
        String body = "Uma reserva foi confirmada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "Hóspedes: " + reservation.guests() + "\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        String subject = "Reserva cancelada - quarto " + reservation.roomNumber();
        String body = "Uma reserva foi cancelada:\n\n"
                + "Quarto: " + reservation.roomNumber() + "\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                + "\nO quarto está disponível novamente para essas datas.\n";
        emailSender.send(HOUSEKEEPING_EMAIL, subject, body);
    }
}
