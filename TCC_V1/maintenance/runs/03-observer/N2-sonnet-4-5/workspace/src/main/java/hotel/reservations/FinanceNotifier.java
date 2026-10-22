package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class FinanceNotifier implements ReservationListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

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
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, boolean wasConfirmed) {
        if (!wasConfirmed) {
            return;
        }
        BigDecimal refund = reservation.refund();
        if (refund.signum() == 0) {
            return;
        }
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Reembolso:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(refund) + "\n";
        emailSender.send(FINANCE_EMAIL, subject, body);
    }
}
