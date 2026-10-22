package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** Notifies the finance department about payments and refunds. */
public final class FinanceNotifier implements ReservationEventListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Um pagamento foi recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, boolean wasPreviouslyConfirmed) {
        if (!wasPreviouslyConfirmed || reservation.refund().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Um reembolso precisa ser processado:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
