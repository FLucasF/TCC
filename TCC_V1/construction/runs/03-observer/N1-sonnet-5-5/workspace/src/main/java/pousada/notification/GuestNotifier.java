package pousada.notification;

import pousada.app.*;
import pousada.domain.Document;
import pousada.domain.Reservation;

public class GuestNotifier implements EventSubscriber {
    private final MessageSender sender;

    public GuestNotifier(MessageSender sender) {
        this.sender = sender;
    }

    @Override
    public void subscribeTo(EventBus bus) {
        bus.subscribe(ReservationConfirmed.class, this::onConfirmed);
    }

    private void onConfirmed(ReservationConfirmed event) {
        Reservation r = event.reservation();
        String body = "Olá, " + r.guest().name() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Document.of(r).render() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        sender.send(Message.Channel.EMAIL, r.guest().email(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
