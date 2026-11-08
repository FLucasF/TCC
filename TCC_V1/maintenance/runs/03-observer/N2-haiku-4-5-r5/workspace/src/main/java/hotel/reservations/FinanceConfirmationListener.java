package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;

final class FinanceConfirmationListener implements ReservationListener {

    private final EmailSender emailSender;

    FinanceConfirmationListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.reservation();
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Reserva " + reservation.id() + "\n\n"
                + "Valor pago: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send("financeiro@pousadamarealta.com.br", subject, body);
    }

    @Override
    public void onReservationCancelled(ReservationCancelledEvent event) {
    }
}
