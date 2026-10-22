package pousada.notification;

import pousada.app.*;
import pousada.domain.Formats;
import pousada.domain.Reservation;

public class FinanceNotifier implements EventSubscriber {
    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final MessageSender sender;

    public FinanceNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void subscribeTo(EventBus bus) {
        bus.subscribe(ReservationConfirmed.class, e -> onConfirmed(e.reservation()));
        bus.subscribe(ConfirmedReservationCancelled.class, e -> onCancelled(e.reservation()));
    }

    private void onConfirmed(Reservation r) {
        sender.send(Message.Channel.EMAIL, ADDRESS, "Pagamento recebido - reserva " + r.id(),
                "Recebemos o pagamento da reserva nº " + r.id() + ": " + Formats.money(r.amountPaid()) + ".");
    }

    private void onCancelled(Reservation r) {
        if (r.refund().signum() == 0) {
            return;
        }
        sender.send(Message.Channel.EMAIL, ADDRESS, "Reembolso - reserva " + r.id(),
                "A reserva nº " + r.id() + " foi cancelada. Valor a devolver: " + Formats.money(r.refund()) + ".");
    }
}
