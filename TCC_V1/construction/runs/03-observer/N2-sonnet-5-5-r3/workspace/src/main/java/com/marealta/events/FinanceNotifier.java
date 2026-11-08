package com.marealta.events;

import com.marealta.domain.Channel;
import com.marealta.domain.MessageOutbox;
import com.marealta.domain.Money;
import com.marealta.domain.Reservation;

/** O financeiro vê os pagamentos recebidos e os reembolsos a devolver. */
public final class FinanceNotifier implements ReservationListener {
    private final MessageOutbox outbox;
    private final String address;

    public FinanceNotifier(MessageOutbox outbox, String address) {
        this.outbox = outbox;
        this.address = address;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        Reservation r = event.reservation();
        switch (event.type()) {
            case CONFIRMED -> outbox.send(Channel.EMAIL, address, "Pagamento recebido - reserva " + r.id(),
                    "Recebemos o pagamento da reserva " + r.id() + ".\n"
                            + "Valor pago: " + Money.brl(r.amountPaid()));
            case CANCELLED -> {
                if (r.refund() != null && r.refund().signum() > 0) {
                    outbox.send(Channel.EMAIL, address, "Reembolso - reserva " + r.id(),
                            "A reserva " + r.id() + " foi cancelada.\n"
                                    + "Valor a devolver: " + Money.brl(r.refund()));
                }
            }
            default -> { }
        }
    }
}
