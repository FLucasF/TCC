package br.com.marealta;

import java.util.List;

/** Avisa o financeiro dos pagamentos recebidos e dos reembolsos a fazer. */
public class FinanceNotifier implements ReservationListener {
    private final String address;

    public FinanceNotifier(String address) {
        this.address = address;
    }

    @Override
    public List<Message> confirmed(Reservation r) {
        String body = "Pagamento recebido da reserva nº " + r.id() + ".\n"
                + "Valor pago: " + Format.money(r.amountPaid());
        return List.of(Message.email(address, "Pagamento recebido - reserva " + r.id(), body));
    }

    @Override
    public List<Message> cancelledAfterConfirmation(Reservation r) {
        if (r.refund().signum() <= 0) return List.of();
        String body = "A reserva nº " + r.id() + " foi cancelada.\n"
                + "Valor a devolver: " + Format.money(r.refund());
        return List.of(Message.email(address, "Reembolso - reserva " + r.id(), body));
    }
}
