package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;

/** Tells finance about the money that comes in and the money that has to go back. */
public final class FinanceNotices implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public FinanceNotices(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void confirmed(Reservation reservation) {
        emailSender.send(
                address,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void cancelledAfterConfirmation(Reservation reservation) {
        if (reservation.refund().signum() == 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
