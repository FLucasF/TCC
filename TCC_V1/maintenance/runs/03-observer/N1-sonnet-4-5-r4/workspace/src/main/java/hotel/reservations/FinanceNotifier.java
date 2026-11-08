package hotel.reservations;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies finance when payments are received or refunds are issued. */
final class FinanceNotifier implements ReservationEventListener {

    private final EmailSender emailSender;

    FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                "financeiro@pousadamarealta.com.br",
                "Pagamento recebido - reserva " + reservation.id(),
                "Pagamento recebido:\n\n"
                        + "Reserva: " + reservation.id() + "\n"
                        + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n");
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        if (reservation.refund() != null && reservation.refund().compareTo(BigDecimal.ZERO) > 0) {
            emailSender.send(
                    "financeiro@pousadamarealta.com.br",
                    "Reembolso - reserva " + reservation.id(),
                    "Reembolso a processar:\n\n"
                            + "Reserva: " + reservation.id() + "\n"
                            + "Valor: " + TextFormat.money(reservation.refund()) + "\n");
        }
    }
}
