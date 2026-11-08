package hotel.notifications;

import static hotel.documents.TextFormat.money;

import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Sends notifications to the finance department about payments and refunds. */
public final class FinanceNotifications implements ReservationEventListener {

    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifications(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + money(reservation.amountPaid()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund().compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Reembolso a processar:\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor: " + money(reservation.refund()) + "\n";
            emailSender.send(EMAIL, subject, body);
        }
    }
}
