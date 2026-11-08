package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

/** Notifies housekeeping when a reservation is confirmed or cancelled. */
final class HousekeepingNotifier implements ReservationEventListener {

    private final EmailSender emailSender;

    HousekeepingNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                "governanca@pousadamarealta.com.br",
                "Reserva confirmada - quarto " + reservation.roomNumber(),
                "Nova reserva confirmada:\n\n"
                        + "Quarto: " + reservation.roomNumber() + "\n"
                        + "Entrada: " + TextFormat.date(reservation.checkIn()) + "\n"
                        + "Saída: " + TextFormat.date(reservation.checkOut()) + "\n"
                        + "Hóspedes: " + reservation.guests() + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        if (reservation.amountPaid().signum() > 0) {
            emailSender.send(
                    "governanca@pousadamarealta.com.br",
                    "Reserva cancelada - quarto " + reservation.roomNumber(),
                    "Reserva cancelada:\n\n"
                            + "Quarto: " + reservation.roomNumber() + "\n"
                            + "Datas que ficaram livres:\n"
                            + "  De: " + TextFormat.date(reservation.checkIn()) + "\n"
                            + "  Até: " + TextFormat.date(reservation.checkOut()) + "\n");
        }
    }
}
