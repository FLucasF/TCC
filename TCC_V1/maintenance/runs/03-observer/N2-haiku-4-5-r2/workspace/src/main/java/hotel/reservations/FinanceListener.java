package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

/** Notifies the finance department of confirmed reservations and refunds. */
public final class FinanceListener implements ReservationEventListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
        Reservation reservation = event.reservation();
        BigDecimal refund = event.refund();
        if (refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Valor: " + TextFormat.money(refund);
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
