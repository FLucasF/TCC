package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.events.DomainEvent;
import br.com.pousadamarealta.reservas.events.EventSubscriber;
import br.com.pousadamarealta.reservas.events.ReservationCancelled;
import br.com.pousadamarealta.reservas.events.ReservationConfirmed;

/** Avisa a governança para preparar o quarto (confirmação) ou saber que ele ficou livre (cancelamento). */
public class GovernanceNotifier implements EventSubscriber {
    public static final String ADDRESS = "governanca@pousadamarealta.com.br";

    private final MessageGateway gateway;

    public GovernanceNotifier(MessageGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void on(DomainEvent event) {
        if (event instanceof ReservationConfirmed e) {
            Reservation r = e.reservation();
            gateway.sendEmail(ADDRESS, "Reserva confirmada - quarto " + e.room().number(),
                    "Reserva nº " + r.id() + " confirmada para o quarto " + e.room().number()
                            + " (" + e.room().type() + ").\n"
                            + "Entrada: " + Brl.date(r.checkIn()) + "\n"
                            + "Saída: " + Brl.date(r.checkOut()) + "\n"
                            + "Hóspedes: " + r.guests());
        } else if (event instanceof ReservationCancelled e) {
            Reservation r = e.reservation();
            gateway.sendEmail(ADDRESS, "Reserva cancelada - quarto " + e.room().number(),
                    "A reserva nº " + r.id() + " do quarto " + e.room().number() + " foi cancelada.\n"
                            + "Datas que ficaram livres: de " + Brl.date(r.checkIn())
                            + " (entrada) a " + Brl.date(r.checkOut()) + " (saída).");
        }
    }
}
