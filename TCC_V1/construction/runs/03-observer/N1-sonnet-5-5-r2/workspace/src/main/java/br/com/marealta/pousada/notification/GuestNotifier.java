package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationConfirmed;

/** Envia ao hóspede o comprovante da reserva confirmada. */
public class GuestNotifier implements Notifier {

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
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + r.receipt().toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        sender.send(Channel.EMAIL, r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }
}
