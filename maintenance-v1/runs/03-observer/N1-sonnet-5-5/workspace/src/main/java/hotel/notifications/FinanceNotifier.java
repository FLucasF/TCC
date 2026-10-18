package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Tells the finance team about payments received and refunds to make. */
public final class FinanceNotifier implements ReservationListener {

    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        emailSender.send(
                ADDRESS,
                "Pagamento recebido - reserva " + reservation.id(),
                "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void confirmedReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund().signum() > 0) {
            emailSender.send(
                    ADDRESS,
                    "Reembolso - reserva " + reservation.id(),
                    "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
        }
    }
}
