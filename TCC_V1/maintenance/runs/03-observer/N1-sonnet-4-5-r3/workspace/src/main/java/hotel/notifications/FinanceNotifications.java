package hotel.notifications;

import static hotel.documents.TextFormat.money;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifications for the finance team: payments received and refunds to issue. */
public final class FinanceNotifications implements DepartmentNotifications {

    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor pago: " + money(reservation.amountPaid()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        BigDecimal refund = reservation.refund();
        if (refund != null && refund.compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Reembolso a processar.\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor a devolver: " + money(refund) + "\n";
            emailSender.send(EMAIL, subject, body);
        }
    }
}
