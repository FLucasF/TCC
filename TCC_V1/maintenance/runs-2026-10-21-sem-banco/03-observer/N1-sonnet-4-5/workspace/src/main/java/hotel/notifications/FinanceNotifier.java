package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class FinanceNotifier implements ReservationEventListener {

    private final EmailSender emailSender;
    private final String email;

    public FinanceNotifier(EmailSender emailSender, String email) {
        this.emailSender = emailSender;
        this.email = email;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(email, subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room, BigDecimal refund) {
        if (refund.signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Reembolso necessário:\n\n"
                    + "Reserva: " + reservation.id() + "\n"
                    + "Valor: " + TextFormat.money(refund) + "\n";
            emailSender.send(email, subject, body);
        }
    }
}
