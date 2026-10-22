package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies finance when payments are received or refunds are due. */
public final class FinanceNotifier implements ReservationListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        if (reservation.refund() == null || reservation.refund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Reembolso a processar:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
