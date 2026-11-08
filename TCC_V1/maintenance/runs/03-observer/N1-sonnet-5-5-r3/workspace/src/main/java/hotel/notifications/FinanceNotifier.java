package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.ReservationEvent;
import hotel.reservations.ReservationListener;

/** Tells finance about payments received and refunds to be made. */
public final class FinanceNotifier implements ReservationListener {

    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void on(ReservationEvent event) {
        long id = event.reservation().id();
        switch (event) {
            case ReservationEvent.Confirmed confirmed -> emailSender.send(
                    ADDRESS,
                    "Pagamento recebido - reserva " + id,
                    "Valor pago: " + TextFormat.money(confirmed.reservation().amountPaid()) + "\n");
            case ReservationEvent.CancelledAfterPayment cancelled -> {
                if (cancelled.refund().signum() > 0) {
                    emailSender.send(
                            ADDRESS,
                            "Reembolso - reserva " + id,
                            "Valor a devolver: " + TextFormat.money(cancelled.refund()) + "\n");
                }
            }
        }
    }
}
