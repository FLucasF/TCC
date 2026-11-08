package hotel.notifications;

import static hotel.documents.TextFormat.money;

import hotel.messages.EmailSender;
import hotel.reservations.ReservationListener;

/** Tells finance about payments received and refunds to make. */
public final class FinanceNotifier implements ReservationListener {

    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Confirmed event) {
        long id = event.reservation().id();
        emailSender.send(
                ADDRESS,
                "Pagamento recebido - reserva " + id,
                "Pagamento recebido da reserva " + id + ".\n"
                        + "Valor pago: " + money(event.reservation().amountPaid()) + "\n");
    }

    @Override
    public void onCancelled(Cancelled event) {
        if (!event.wasConfirmed() || event.refund().signum() <= 0) {
            return;
        }
        long id = event.reservation().id();
        emailSender.send(
                ADDRESS,
                "Reembolso - reserva " + id,
                "A reserva " + id + " foi cancelada.\n"
                        + "Valor a devolver: " + money(event.refund()) + "\n");
    }
}
