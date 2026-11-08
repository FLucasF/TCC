package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

public final class FinanceListener implements ReservationEventListener {

    private final EmailSender emailSender;

    public FinanceListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(Reservation reservation, Room room) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Valor: " + TextFormat.money(reservation.amountPaid());
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(Reservation reservation, Room room) {
        if (reservation.refund().signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Valor a devolver: " + TextFormat.money(reservation.refund());
            emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
        }
    }
}
