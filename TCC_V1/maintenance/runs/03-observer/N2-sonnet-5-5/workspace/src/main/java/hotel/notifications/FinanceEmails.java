package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

/** Tells finance about payments received and refunds to be made. */
public final class FinanceEmails implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public FinanceEmails(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, Room room, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED || reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
