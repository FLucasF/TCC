package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

/** Notifies finance when a reservation is confirmed or cancelled. */
public final class FinanceNotifier implements ReservationEventHandler {

    private static final String EMAIL = "financeiro@pousadamarealta.com.br";
    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        if (reservation.refund() == null || reservation.refund().signum() == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.refund()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }
}
