package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Receipt;
import br.com.pousadamarealta.reservas.events.DomainEvent;
import br.com.pousadamarealta.reservas.events.EventSubscriber;
import br.com.pousadamarealta.reservas.events.ReservationConfirmed;

/** Avisa o hóspede, no e-mail dele, que a reserva foi confirmada. */
public class GuestNotifier implements EventSubscriber {
    private final MessageGateway gateway;

    public GuestNotifier(MessageGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void on(DomainEvent event) {
        if (event instanceof ReservationConfirmed e) {
            String body = "Olá, " + e.reservation().guestName() + "!\n\n"
                    + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                    + ReceiptText.of(Receipt.of(e.reservation(), e.room())) + "\n\n"
                    + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                    + "Até breve!";
            gateway.sendEmail(e.reservation().guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
        }
    }
}
