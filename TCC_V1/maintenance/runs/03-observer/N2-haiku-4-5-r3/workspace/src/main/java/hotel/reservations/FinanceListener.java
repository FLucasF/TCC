package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

public final class FinanceListener implements ReservationEventListener {

    private final EmailSender emailSender;

    public FinanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation) {
        String reservationId = String.valueOf(reservation.id());
        String subject = "Pagamento recebido - reserva " + reservationId;
        String body = "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation) {
        BigDecimal refund = reservation.refund();
        if (refund == null || refund.signum() <= 0) {
            return;
        }

        String reservationId = String.valueOf(reservation.id());
        String subject = "Reembolso - reserva " + reservationId;
        String body = "Valor: " + TextFormat.money(refund) + "\n";
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }
}
