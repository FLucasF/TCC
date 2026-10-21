package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

public final class FinanceNotifier implements ReservationNotifier {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation) {
        if (reservation.refund() == null || reservation.refund().signum() == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.refund());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
