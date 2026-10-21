package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;

public class FinanceNotifier implements ReservationListener {
    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";
    private final Outbox outbox;

    public FinanceNotifier(Outbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void on(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed(Reservation r) -> outbox.sendEmail(ADDRESS,
                    "Pagamento recebido - reserva " + r.id(),
                    "Pagamento recebido da reserva nº " + r.id() + " (quarto " + r.room().number() + ").\n"
                            + "Valor pago: " + Money.format(r.amountPaid()));
            case ReservationEvent.CancelledAfterConfirmation(Reservation r) -> {
                if (r.refund().signum() > 0) {
                    outbox.sendEmail(ADDRESS,
                            "Reembolso - reserva " + r.id(),
                            "Reserva nº " + r.id() + " cancelada (quarto " + r.room().number() + ").\n"
                                    + "Valor a devolver: " + Money.format(r.refund()));
                }
            }
        }
    }
}
