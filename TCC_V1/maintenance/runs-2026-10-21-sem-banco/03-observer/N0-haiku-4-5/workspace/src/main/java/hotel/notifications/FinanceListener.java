package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies finance about payment received and refunds. */
public final class FinanceListener implements ReservationEventListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        BigDecimal refund = reservation.refund();
        if (refund != null && refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Valor: " + TextFormat.money(refund);
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
