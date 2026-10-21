package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationListener;
import br.com.marealta.reservas.domain.ReservationStatus;

public class FinanceNotifier implements ReservationListener {
    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";
    private final MessageOutbox outbox;

    public FinanceNotifier(MessageOutbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void onConfirmed(Reservation r) {
        outbox.email(ADDRESS, "Pagamento recebido - reserva " + r.id(),
                "Pagamento recebido da reserva " + r.id() + ".\n"
                        + "Valor pago: " + Money.format(r.amountPaid()));
    }

    @Override
    public void onCancelled(Reservation r, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED || r.refund().signum() == 0) return;
        outbox.email(ADDRESS, "Reembolso - reserva " + r.id(),
                "A reserva " + r.id() + " foi cancelada.\n"
                        + "Valor a devolver: " + Money.format(r.refund()));
    }
}
