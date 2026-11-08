package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.persistence.MessageRepository;

/** Avisa o financeiro do pagamento recebido e do reembolso a fazer. */
public class FinanceNotifier implements ReservationListener {
    private final MessageRepository outbox;
    private final String address;

    public FinanceNotifier(MessageRepository outbox, String address) {
        this.outbox = outbox;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation r) {
        outbox.send(Message.Channel.EMAIL, address, "Pagamento recebido - reserva " + r.id(),
                "Pagamento recebido da reserva " + r.id() + ": " + Money.brl(r.amountPaid()) + ".");
    }

    @Override
    public void onCancelled(Reservation r, ReservationStatus previousStatus) {
        if (previousStatus != ReservationStatus.CONFIRMED || r.refund().signum() <= 0) {
            return;
        }
        outbox.send(Message.Channel.EMAIL, address, "Reembolso - reserva " + r.id(),
                "Reembolso a fazer da reserva " + r.id() + ": " + Money.brl(r.refund()) + ".");
    }
}
