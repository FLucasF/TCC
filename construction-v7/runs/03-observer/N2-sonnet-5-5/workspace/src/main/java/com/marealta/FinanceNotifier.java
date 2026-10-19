package com.marealta;

/** Avisa o financeiro de pagamentos recebidos e de reembolsos a fazer. */
public class FinanceNotifier implements ReservationListener {
    static final String ADDRESS = "financeiro@pousadamarealta.com.br";
    private final Outbox outbox;

    public FinanceNotifier(Outbox outbox) { this.outbox = outbox; }

    @Override
    public void onConfirmed(Reservation r) {
        outbox.send(Channel.EMAIL, ADDRESS, "Pagamento recebido - reserva " + r.id(),
                "Foi recebido o pagamento da reserva " + r.id() + ": " + Money.brl(r.amountPaid()) + ".");
    }

    @Override
    public void onPaidReservationCancelled(Reservation r) {
        if (r.refund().signum() <= 0) return;
        outbox.send(Channel.EMAIL, ADDRESS, "Reembolso - reserva " + r.id(),
                "A reserva " + r.id() + " foi cancelada. Valor a devolver: " + Money.brl(r.refund()) + ".");
    }
}
