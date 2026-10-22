package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Notifies the finance team about reservation events. */
public final class FinanceNotifier implements ReservationNotifier {

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onConfirmed(Reservation reservation, Room room) {
        emailSender.send(
                "financeiro@pousadamarealta.com.br",
                "Pagamento recebido - reserva " + reservation.id(),
                buildPaymentReceivedBody(reservation));
    }

    @Override
    public void onCancelled(Reservation reservation, Room room) {
        if (reservation.refund().signum() > 0) {
            emailSender.send(
                    "financeiro@pousadamarealta.com.br",
                    "Reembolso - reserva " + reservation.id(),
                    buildRefundBody(reservation));
        }
    }

    private static String buildPaymentReceivedBody(Reservation reservation) {
        return "Pagamento recebido da reserva nº " + reservation.id() + ".\n\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
    }

    private static String buildRefundBody(Reservation reservation) {
        return "Reembolso da reserva nº " + reservation.id() + ".\n\n"
                + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
    }
}
