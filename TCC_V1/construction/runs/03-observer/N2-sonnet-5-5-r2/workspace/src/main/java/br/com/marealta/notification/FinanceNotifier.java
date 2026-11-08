package br.com.marealta.notification;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

/** Avisa o financeiro dos pagamentos recebidos e dos reembolsos a fazer. */
public final class FinanceNotifier implements ReservationListener {

    private final MessageSender sender;
    private final String address;

    public FinanceNotifier(MessageSender sender, String address) {
        this.sender = sender;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation r) {
        String body = "Recebemos o pagamento da reserva nº " + r.id() + " (" + r.guestName() + ").\n"
                + "Valor pago: " + Money.format(r.amountPaid());
        sender.send(Message.email(address, "Pagamento recebido - reserva " + r.id(), body));
    }

    @Override
    public void onCancelled(Reservation r) {
        if (!r.hasPayment() || r.refund().signum() <= 0) {
            return;
        }
        String body = "A reserva nº " + r.id() + " (" + r.guestName() + ") foi cancelada.\n"
                + "Valor a devolver: " + Money.format(r.refund());
        sender.send(Message.email(address, "Reembolso - reserva " + r.id(), body));
    }
}
