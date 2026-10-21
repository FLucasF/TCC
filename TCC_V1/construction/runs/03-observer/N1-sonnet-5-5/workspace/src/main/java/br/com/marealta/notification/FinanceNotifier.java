package br.com.marealta.notification;

import br.com.marealta.domain.Formats;
import br.com.marealta.domain.Reservation;

/** Avisa o financeiro dos pagamentos recebidos e dos reembolsos a fazer. */
public class FinanceNotifier implements ReservationListener {
    static final String ADDRESS = "financeiro@pousadamarealta.com.br";

    private final MessageSender sender;

    public FinanceNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void on(ReservationEvent event) {
        switch (event) {
            case ReservationEvent.Confirmed confirmed -> {
                Reservation r = confirmed.reservation();
                sender.email(ADDRESS, "Pagamento recebido - reserva " + r.id(),
                        "Pagamento recebido da reserva %d: %s.".formatted(r.id(), Formats.money(r.amountPaid())));
            }
            case ReservationEvent.Cancelled cancelled when cancelled.reservation().refund().signum() > 0 -> {
                Reservation r = cancelled.reservation();
                sender.email(ADDRESS, "Reembolso - reserva " + r.id(),
                        "Reserva %d cancelada. Valor a devolver ao hóspede: %s.".formatted(
                                r.id(), Formats.money(r.refund())));
            }
            default -> {
            }
        }
    }
}
