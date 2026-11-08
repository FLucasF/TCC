package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies the finance department when a payment is received or a refund is due. */
public final class FinanceNotificationListener implements ReservationEventListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Um pagamento foi recebido.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        BigDecimal refund = reservation.refund();
        if (refund != null && refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Um reembolso deve ser feito.\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor a devolver: " + TextFormat.money(refund) + "\n";
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
