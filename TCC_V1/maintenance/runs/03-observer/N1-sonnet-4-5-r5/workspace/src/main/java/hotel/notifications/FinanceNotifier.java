package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies the finance team about payments and refunds. */
public final class FinanceNotifier implements ReservationEventListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Um pagamento foi recebido.\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        if (reservation.status() == ReservationStatus.CANCELLED
                && reservation.refund() != null
                && reservation.refund().compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Uma reserva foi cancelada e há valor a devolver.\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n";
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
