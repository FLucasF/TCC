package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.ConfirmedReservationCancelled;
import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationConfirmed;

/** Avisa o financeiro dos pagamentos recebidos e dos reembolsos a devolver. */
public class FinanceNotifier implements Notifier {

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
        String body = "Pagamento recebido da reserva nº " + r.id() + " (" + r.guestName() + "): "
                + Money.format(r.amountPaid());
        sender.send(Channel.EMAIL, ADDRESS, "Pagamento recebido - reserva " + r.id(), body);
    }

    private void onCancelled(Reservation r) {
        if (r.refund().signum() == 0) {
            return;
        }
        String body = "A reserva nº " + r.id() + " (" + r.guestName() + ") foi cancelada.\n"
                + "Valor a devolver: " + Money.format(r.refund());
        sender.send(Channel.EMAIL, ADDRESS, "Reembolso - reserva " + r.id(), body);
    }
}
