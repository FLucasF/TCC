package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

/** Notifies finance about payments received and refunds due. */
public final class FinanceNotifier implements ReservationListener {

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
    public void onReservationCancelled(Reservation reservation) {
        if (reservation.refund() != null && reservation.refund().compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Um reembolso precisa ser processado:\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
