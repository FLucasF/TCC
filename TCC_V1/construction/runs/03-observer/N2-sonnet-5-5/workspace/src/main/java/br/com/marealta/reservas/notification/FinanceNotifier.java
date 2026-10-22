package br.com.marealta.reservas.notification;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Reservation;

/** Avisa o financeiro dos pagamentos recebidos e dos reembolsos a fazer. */
public class FinanceNotifier implements ReservationListener {
    private final MessageLog log;
    private final String address;

    public FinanceNotifier(MessageLog log, String address) {
        this.log = log;
        this.address = address;
    }

    @Override
    public void onConfirmed(ReservationConfirmed event) {
        Reservation r = event.reservation();
        String body = "Pagamento recebido da reserva nº " + r.id() + " (" + r.guestName() + "): "
                + Money.brl(r.amountPaid()) + ".";
        log.sendEmail(address, "Pagamento recebido - reserva " + r.id(), body);
    }

    @Override
    public void onCancelled(ReservationCancelled event) {
        Reservation r = event.reservation();
        if (r.refund() == null || r.refund().signum() <= 0) {
            return;
        }
        String body = "A reserva nº " + r.id() + " (" + r.guestName() + ") foi cancelada. "
                + "Valor a devolver: " + Money.brl(r.refund()) + ".";
        log.sendEmail(address, "Reembolso - reserva " + r.id(), body);
    }
}
