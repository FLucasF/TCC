package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationEventListener;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies finance about payments and refunds. */
public final class FinanceNotifier implements ReservationEventListener {

    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund().compareTo(BigDecimal.ZERO) > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Reembolso a processar:\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
            emailSender.send(EMAIL, subject, body);
        }
    }
}
