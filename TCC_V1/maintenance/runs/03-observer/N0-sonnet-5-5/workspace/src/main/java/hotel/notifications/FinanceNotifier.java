package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Tells finance (financeiro) about payments received and refunds to pay back. */
public final class FinanceNotifier implements ReservationListener {

    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                ADDRESS,
                "Pagamento recebido - reserva " + reservation.id(),
                "Pagamento recebido da reserva " + reservation.id() + ".\n"
                        + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onConfirmedReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                ADDRESS,
                "Reembolso - reserva " + reservation.id(),
                "A reserva " + reservation.id() + " foi cancelada.\n"
                        + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
