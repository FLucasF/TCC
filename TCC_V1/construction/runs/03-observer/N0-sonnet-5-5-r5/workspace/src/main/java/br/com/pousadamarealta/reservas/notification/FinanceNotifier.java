package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.events.DomainEvent;
import br.com.pousadamarealta.reservas.events.EventSubscriber;
import br.com.pousadamarealta.reservas.events.ReservationCancelled;
import br.com.pousadamarealta.reservas.events.ReservationConfirmed;

/** Avisa o financeiro de pagamentos recebidos e de reembolsos a fazer. */
public class FinanceNotifier implements EventSubscriber {
    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final MessageGateway gateway;

    public FinanceNotifier(MessageGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void on(DomainEvent event) {
        if (event instanceof ReservationConfirmed e) {
            Reservation r = e.reservation();
            gateway.sendEmail(ADDRESS, "Pagamento recebido - reserva " + r.id(),
                    "Pagamento recebido da reserva nº " + r.id() + " (quarto " + e.room().number() + ").\n"
                            + "Valor pago: " + Brl.money(r.amountPaid()));
        } else if (event instanceof ReservationCancelled e && e.reservation().refund().signum() > 0) {
            Reservation r = e.reservation();
            gateway.sendEmail(ADDRESS, "Reembolso - reserva " + r.id(),
                    "A reserva nº " + r.id() + " (quarto " + e.room().number() + ") foi cancelada.\n"
                            + "Valor a devolver: " + Brl.money(r.refund()));
        }
    }
}
