package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;

/** Tells finance about payments received and refunds to make. */
public final class FinanceNotifier implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public FinanceNotifier(EmailSender emailSender, String address) {
        this.emailSender = emailSender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation reservation) {
        emailSender.send(
                address,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, boolean wasConfirmed) {
        if (!wasConfirmed || reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
