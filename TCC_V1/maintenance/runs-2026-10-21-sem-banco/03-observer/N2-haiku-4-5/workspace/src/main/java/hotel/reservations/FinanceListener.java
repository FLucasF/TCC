package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

/** Notifies the finance department of confirmations and refunds. */
public final class FinanceListener implements ReservationEventListener {

    private final EmailSender emailSender;

    public FinanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        BigDecimal refund = reservation.refund();
        if (refund != null && refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Valor: " + TextFormat.money(refund);
            emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
        }
    }
}
