package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

public final class FinanceNotifier implements ReservationEventListener {

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
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
        if (reservation.refund().signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Valor: " + TextFormat.money(reservation.refund());
            emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
        }
    }
}
