package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

public final class FinanceNotifier implements ReservationEventListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, int roomNumber) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Um pagamento foi recebido.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, int roomNumber, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }
        BigDecimal refund = reservation.refund();
        if (refund == null || refund.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Uma reserva foi cancelada com reembolso.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor a devolver: " + TextFormat.money(refund) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
