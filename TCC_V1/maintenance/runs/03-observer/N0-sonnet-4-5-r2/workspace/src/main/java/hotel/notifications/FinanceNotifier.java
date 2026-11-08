package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** Notifies finance when payments are received or refunds are needed. */
public final class FinanceNotifier implements ReservationEventListener {

    private static final String EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotifier(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        switch (event) {
            case ReservationConfirmed confirmed -> notifyPayment(confirmed.reservation());
            case ReservationCancelled cancelled -> {
                if (cancelled.wasConfirmed() && hasRefund(cancelled.reservation())) {
                    notifyRefund(cancelled.reservation());
                }
            }
        }
    }

    private void notifyPayment(Reservation reservation) {
        String subject = "Pagamento recebido - reserva " + reservation.id();
        String body = "Pagamento recebido:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.amountPaid()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    private void notifyRefund(Reservation reservation) {
        String subject = "Reembolso - reserva " + reservation.id();
        String body = "Reembolso necessário:\n\n"
                + "Reserva: " + reservation.id() + "\n"
                + "Valor: " + TextFormat.money(reservation.refund()) + "\n";
        emailSender.send(EMAIL, subject, body);
    }

    private boolean hasRefund(Reservation reservation) {
        return reservation.refund() != null
                && reservation.refund().compareTo(BigDecimal.ZERO) > 0;
    }
}
