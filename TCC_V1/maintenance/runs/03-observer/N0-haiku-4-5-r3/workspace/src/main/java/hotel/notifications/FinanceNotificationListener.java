package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class FinanceNotificationListener implements ReservationNotificationListener {

    private static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
    private final EmailSender emailSender;

    public FinanceNotificationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = String.format(
                "Reserva nº %d confirmada.%n" + "Valor: %s",
                reservation.id(), TextFormat.money(reservation.amountPaid()));
        emailSender.send(FINANCE_EMAIL, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        BigDecimal refund = reservation.refund();
        if (refund != null && refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = String.format(
                    "Reserva nº %d cancelada.%n" + "Valor a devolver: %s",
                    reservation.id(), TextFormat.money(refund));
            emailSender.send(FINANCE_EMAIL, subject, body);
        }
    }
}
