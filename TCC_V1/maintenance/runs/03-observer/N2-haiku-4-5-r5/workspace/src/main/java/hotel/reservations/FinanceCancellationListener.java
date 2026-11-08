package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import java.math.BigDecimal;

final class FinanceCancellationListener implements ReservationListener {

    private final EmailSender emailSender;

    FinanceCancellationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
        Reservation reservation = event.reservation();
        if (reservation.refund().signum() > 0) {
            String subject = "Reembolso - reserva " + reservation.id();
            String body = "Reserva " + reservation.id() + "\n\n"
                    + "Valor a devolver: " + TextFormat.money(reservation.refund()) + "\n";
            emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
        }
    }
}
