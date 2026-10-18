package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationObserver;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies finance when payments are received or refunds are due. */
public final class FinanceNotifier implements ReservationObserver {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Um pagamento foi recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        BigDecimal refund = reservation.refund();
        if (refund != null && refund.compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Um reembolso deve ser feito:\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor a devolver: " + TextFormat.money(refund) + "\n";
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
