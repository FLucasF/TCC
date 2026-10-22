package br.com.marealta.notify;

import br.com.marealta.domain.Receipt;
import br.com.marealta.event.DomainEvent;
import br.com.marealta.event.DomainEvent.ReservationConfirmed;
import br.com.marealta.event.Message;
import br.com.marealta.event.Subscriber;

import java.util.List;

/** Avisa o hóspede da confirmação, com o comprovante. */
public class GuestNotifier implements Subscriber {
    @Override
    public List<Message> on(DomainEvent event) {
        if (!(event instanceof ReservationConfirmed e)) return List.of();
        String body = "Olá, " + e.reservation().guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(e.reservation(), e.room()).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return List.of(Message.email(e.reservation().guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
