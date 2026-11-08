package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Tells the finance team about payments received and refunds to pay back. */
public final class Finance implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public Finance(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Pagamento recebido - reserva " + reservation.id(),
                "Pagamento da reserva " + reservation.id() + " recebido.\n"
                        + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onConfirmedReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund() == null || reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "A reserva " + reservation.id() + " foi cancelada.\n"
                        + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
