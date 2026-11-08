package br.com.marealta.pousada.notification;

import java.math.BigDecimal;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationListener;
import br.com.marealta.pousada.domain.ReservationStatus;

/** Avisa o financeiro dos pagamentos recebidos e dos reembolsos a fazer. */
public class FinanceNotice implements ReservationListener {

    private final MessageLog log;
    private final String address;

    public FinanceNotice(MessageLog log, String address) {
        this.log = log;
        this.address = address;
    }

    @Override
    public void onConfirmed(Reservation r) {
        log.send(Channel.EMAIL, address, "Pagamento recebido - reserva " + r.id(),
                "Recebemos o pagamento da reserva " + r.id() + ".\n"
                        + "Valor pago: " + Money.format(r.amountPaid()));
    }

    @Override
    public void onCancelled(Reservation r, ReservationStatus previous) {
        if (previous != ReservationStatus.CONFIRMED || r.refund().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        log.send(Channel.EMAIL, address, "Reembolso - reserva " + r.id(),
                "A reserva " + r.id() + " foi cancelada.\n"
                        + "Valor a devolver: " + Money.format(r.refund()));
    }
}
