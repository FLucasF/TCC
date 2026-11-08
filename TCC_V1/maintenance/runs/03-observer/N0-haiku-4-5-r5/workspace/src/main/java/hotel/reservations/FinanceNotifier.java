package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

public final class FinanceNotifier implements ReservationNotifier {

    static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        if (reservation.status() != ReservationStatus.CANCELLED) {
            return;
        }
        if (reservation.amountPaid().signum() <= 0) {
            return;
        }
        BigDecimal refund = reservation.refund();
        if (refund == null || refund.signum() <= 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Valor a devolver: " + TextFormat.money(refund);
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
