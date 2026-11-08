package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;

/** Tells finance about the payments received and the refunds to make. */
public final class FinanceEmail implements ReservationListener {

    private final EmailSender emailSender;
    private final String address;

    public FinanceEmail(EmailSender emailSender, String address) {
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
    public void cancelled(Reservation reservation, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED || reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                address,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
