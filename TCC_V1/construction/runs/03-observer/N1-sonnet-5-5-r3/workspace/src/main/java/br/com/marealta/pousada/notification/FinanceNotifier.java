package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.util.List;

public class FinanceNotifier implements ReservationListener {

    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    @Override
    public List<Message> onConfirmed(Reservation r, Room room) {
        String body = "Recebemos o pagamento da reserva " + r.id() + ".\n"
                + "Valor pago: " + Money.format(r.amountPaid());
        return List.of(Message.email(ADDRESS, "Pagamento recebido - reserva " + r.id(), body));
    }

    @Override
    public List<Message> onPaidReservationCancelled(Reservation r, Room room) {
        if (r.refund().signum() <= 0) {
            return List.of();
        }
        String body = "A reserva " + r.id() + " foi cancelada e há um reembolso a fazer.\n"
                + "Valor a devolver: " + Money.format(r.refund());
        return List.of(Message.email(ADDRESS, "Reembolso - reserva " + r.id(), body));
    }
}
