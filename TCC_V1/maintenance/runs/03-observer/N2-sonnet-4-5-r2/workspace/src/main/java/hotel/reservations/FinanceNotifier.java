package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies finance when a reservation is confirmed or cancelled. */
public final class FinanceNotifier implements ReservationListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Um pagamento foi recebido.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, boolean wasPaid) {
        if (!wasPaid || reservation.refund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Um reembolso deve ser feito.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
