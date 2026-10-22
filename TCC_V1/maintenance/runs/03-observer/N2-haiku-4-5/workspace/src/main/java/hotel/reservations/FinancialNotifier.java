package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;

public final class FinancialNotifier implements ReservationListener {

    private final EmailSender emailSender;

    public FinancialNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmation(Reservation reservation, Room room) {
        emailSender.send(
                "financeiro@pousadamarealta.com.br",
                "Pagamento recebido - reserva " + reservation.id(),
                confirmationEmailBody(reservation));
    }

    @Override
    public void onCancellation(Reservation reservation, Room room) {
        if (reservation.refund().signum() > 0) {
            emailSender.send(
                    "financeiro@pousadamarealta.com.br",
                    "Reembolso - reserva " + reservation.id(),
                    cancellationEmailBody(reservation));
        }
    }

    private static String confirmationEmailBody(Reservation reservation) {
        return "Valor pago: " + TextFormat.money(reservation.amountPaid());
    }

    private static String cancellationEmailBody(Reservation reservation) {
        return "Valor do reembolso: " + TextFormat.money(reservation.refund());
    }
}
