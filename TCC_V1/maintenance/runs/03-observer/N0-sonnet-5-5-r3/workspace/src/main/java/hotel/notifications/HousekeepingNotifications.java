package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;

/** Housekeeping (governança) prepares the room, so it hears about confirmations and cancellations. */
public final class HousekeepingNotifications implements ReservationListener {

    static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, ReservationReceipt receipt) {
        emailSender.send(
                ADDRESS,
                "Reserva confirmada - quarto " + reservation.roomNumber(),
                "Reserva " + reservation.id() + " confirmada para o quarto " + reservation.roomNumber() + ".\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void confirmedReservationCancelled(Reservation reservation) {
        emailSender.send(
                ADDRESS,
                "Reserva cancelada - quarto " + reservation.roomNumber(),
                "A reserva " + reservation.id() + " do quarto " + reservation.roomNumber() + " foi cancelada.\n"
                        + "O quarto ficou livre de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + ".\n");
    }
}
