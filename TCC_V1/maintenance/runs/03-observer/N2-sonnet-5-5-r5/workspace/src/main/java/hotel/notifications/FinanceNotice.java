package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;

/** Tells finance about payments received and refunds to pay back. */
public final class FinanceNotice implements ReservationListener {

    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotice(EmailSender emailSender) {
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
    public void cancelled(Reservation reservation, Room room, ReservationStatus statusBefore) {
        if (statusBefore != ReservationStatus.CONFIRMED || reservation.refund().signum() <= 0) {
            return;
        }
        emailSender.send(
                ADDRESS,
                "Reembolso - reserva " + reservation.id(),
                "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n");
    }
}
