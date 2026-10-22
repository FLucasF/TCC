package br.com.marealta.notify;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.event.DomainEvent;
import br.com.marealta.event.DomainEvent.ReservationCancelled;
import br.com.marealta.event.DomainEvent.ReservationConfirmed;
import br.com.marealta.event.Message;
import br.com.marealta.event.Subscriber;

import java.util.List;

/** Avisa o financeiro de pagamentos recebidos e reembolsos a fazer. */
public class FinanceNotifier implements Subscriber {
    public static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    @Override
    public List<Message> on(DomainEvent event) {
        Reservation r = event.reservation();
        return switch (event) {
            case ReservationConfirmed e -> List.of(Message.email(ADDRESS, "Pagamento recebido - reserva " + r.id(),
                    "Recebemos o pagamento da reserva " + r.id() + ".\nValor pago: " + Money.brl(r.amountPaid())));
            case ReservationCancelled e when e.refund().signum() > 0 -> List.of(Message.email(ADDRESS,
                    "Reembolso - reserva " + r.id(),
                    "A reserva " + r.id() + " foi cancelada.\nValor a devolver: " + Money.brl(e.refund())));
            case ReservationCancelled e -> List.of();
        };
    }
}
