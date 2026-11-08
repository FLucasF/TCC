package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;

/** Finance (financeiro) hears about payments received and refunds to make. */
public final class FinanceNotifications implements ReservationListener {

    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, ReservationReceipt receipt) {
        emailSender.send(
                ADDRESS,
                "Pagamento recebido - reserva " + reservation.id(),
                "Pagamento da reserva " + reservation.id() + " recebido.\n"
                        + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void confirmedReservationCancelled(Reservation reservation) {
        if (reservation.refund() == null || reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                ADDRESS,
                "Reembolso - reserva " + reservation.id(),
                "A reserva " + reservation.id() + " foi cancelada.\n"
                        + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
