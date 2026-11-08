package br.com.marealta.events;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.OutgoingMessage;
import br.com.marealta.domain.Reservation;

/** Avisa o financeiro de pagamentos recebidos e de reembolsos a devolver. */
public final class FinanceNotifier implements ReservationListener {
    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final MessageSender sender;

    public FinanceNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void onConfirmed(ReservationEvent.Confirmed e) {
        Reservation r = e.reservation();
        String body = "Recebemos o pagamento da reserva nº " + r.id() + " (" + r.guestName() + ").\n"
                + "Valor pago: " + Money.format(r.amountPaid());
        sender.send(OutgoingMessage.email(ADDRESS, "Pagamento recebido - reserva " + r.id(), body));
    }

    @Override
    public void onCancelled(ReservationEvent.Cancelled e) {
        Reservation r = e.reservation();
        if (!e.wasConfirmed() || r.refund() == null || r.refund().signum() <= 0) {
            return;
        }
        String body = "A reserva nº " + r.id() + " (" + r.guestName() + ") foi cancelada.\n"
                + "Valor a devolver: " + Money.format(r.refund());
        sender.send(OutgoingMessage.email(ADDRESS, "Reembolso - reserva " + r.id(), body));
    }
}
