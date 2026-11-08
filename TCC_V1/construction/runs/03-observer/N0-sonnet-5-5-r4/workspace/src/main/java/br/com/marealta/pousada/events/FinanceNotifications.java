package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import java.util.List;

/** Financeiro: quanto entrou na confirmação e quanto precisa ser devolvido no cancelamento. */
public class FinanceNotifications implements NotificationRule {
    private final String address;

    public FinanceNotifications(String address) {
        this.address = address;
    }

    @Override
    public List<Message> messagesFor(ReservationEvent event) {
        Reservation r = event.reservation();
        return switch (event) {
            case ReservationEvent.Confirmed c -> List.of(Message.email(address,
                    "Pagamento recebido - reserva " + r.id(),
                    "Recebemos o pagamento da reserva nº " + r.id() + ".\n"
                            + "Valor pago: " + Money.format(r.amountPaid())));
            case ReservationEvent.Cancelled c when c.wasPaid() && r.refund().signum() > 0 -> List.of(Message.email(address,
                    "Reembolso - reserva " + r.id(),
                    "A reserva nº " + r.id() + " foi cancelada e há valor a devolver.\n"
                            + "Valor a devolver: " + Money.format(r.refund())));
            case ReservationEvent.Cancelled c -> List.of();
        };
    }
}
