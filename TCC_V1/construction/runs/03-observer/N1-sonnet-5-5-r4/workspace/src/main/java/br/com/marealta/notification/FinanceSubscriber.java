package br.com.marealta.notification;

import br.com.marealta.app.Notification;
import br.com.marealta.domain.Formats;
import br.com.marealta.domain.Reservation;

import java.util.List;

/** O financeiro vê os pagamentos que entram e os reembolsos a devolver. */
public class FinanceSubscriber implements Subscriber {
    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    @Override
    public List<Notification> onConfirmed(ReservationEvent.Confirmed event) {
        Reservation r = event.reservation();
        String body = "Pagamento recebido da reserva nº " + r.id() + " (quarto " + r.roomNumber() + ").\n"
                + "Valor pago: " + Formats.money(r.amountPaid());
        return List.of(Notification.email(ADDRESS, "Pagamento recebido - reserva " + r.id(), body));
    }

    @Override
    public List<Notification> onCancelled(ReservationEvent.Cancelled event) {
        Reservation r = event.reservation();
        if (r.refund().signum() == 0) {
            return List.of();
        }
        String body = "A reserva nº " + r.id() + " (quarto " + r.roomNumber() + ") foi cancelada.\n"
                + "Valor a devolver: " + Formats.money(r.refund());
        return List.of(Notification.email(ADDRESS, "Reembolso - reserva " + r.id(), body));
    }
}
