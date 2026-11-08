package hotel.notifications;

import static hotel.documents.TextFormat.money;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

/** Tells finance about payments received and refunds to be made. */
public final class FinanceEmail implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public FinanceEmail(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, Room room, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED || reservation.refund().signum() == 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + money(reservation.refund()) + "\n");
    }
}
