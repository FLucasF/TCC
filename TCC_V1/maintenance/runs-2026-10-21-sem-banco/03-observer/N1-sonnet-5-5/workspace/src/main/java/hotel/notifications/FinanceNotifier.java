package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationEvent;
import hotel.reservations.ReservationListener;

/** Tells the finance team about payments received and refunds to pay back. */
public final class FinanceNotifier implements ReservationListener {

    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void on(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed confirmed -> paymentReceived(confirmed.reservation());
            case ReservationEvent.Cancelled cancelled -> refund(cancelled.reservation());
        }
    }

    private void paymentReceived(Reservation reservation) {
        emailSender.send(
                ADDRESS,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    private void refund(Reservation reservation) {
        if (reservation.refund().signum() > 0) {
            emailSender.send(
                    ADDRESS,
                    "Reembolso - reserva " + reservation.id(),
                    "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
        }
    }
}
