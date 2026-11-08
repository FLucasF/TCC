package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;

/** Finance learns about payments received and refunds to be made. */
final class FinanceNotice implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    FinanceNotice(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        emailSender.send(
                address,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void cancelled(Reservation reservation, Room room, boolean wasConfirmed) {
        if (!wasConfirmed || reservation.refund().signum() == 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
