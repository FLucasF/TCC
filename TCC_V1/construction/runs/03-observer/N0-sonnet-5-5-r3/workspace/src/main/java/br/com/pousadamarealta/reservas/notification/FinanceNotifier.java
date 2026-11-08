package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Money;

import java.util.List;

/** Avisa o financeiro de pagamentos recebidos e de reembolsos a fazer. */
public class FinanceNotifier implements Notifier {
    private final String address;

    public FinanceNotifier(String address) {
        this.address = address;
    }

    @Override
    public List<Message> messagesFor(ReservationEvent event) {
        var reservation = event.reservation();
        return switch (event) {
            case ReservationEvent.Confirmed c -> List.of(Message.email(address,
                    "Pagamento recebido - reserva " + reservation.id(),
                    "Recebemos o pagamento da reserva nº " + reservation.id() + " (" + reservation.guestName()
                            + ").\nValor pago: " + Money.format(reservation.amountPaid())));
            case ReservationEvent.Cancelled c
                    when c.wasConfirmed() && reservation.refund() != null && reservation.refund().signum() > 0 ->
                    List.of(Message.email(address,
                            "Reembolso - reserva " + reservation.id(),
                            "A reserva nº " + reservation.id() + " (" + reservation.guestName()
                                    + ") foi cancelada.\nValor a devolver: " + Money.format(reservation.refund())));
            case ReservationEvent.Cancelled c -> List.of();
        };
    }
}
