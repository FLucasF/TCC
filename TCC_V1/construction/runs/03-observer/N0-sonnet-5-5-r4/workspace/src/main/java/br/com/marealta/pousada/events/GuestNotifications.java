package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Message;
import br.com.marealta.pousada.domain.Receipt;
import java.util.List;

/** Avisa o próprio hóspede, no e-mail dele, quando a reserva é confirmada. */
public class GuestNotifications implements NotificationRule {

    @Override
    public List<Message> messagesFor(ReservationEvent event) {
        if (!(event instanceof ReservationEvent.Confirmed c)) {
            return List.of();
        }
        String body = "Olá, " + c.reservation().guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(c.reservation(), c.room()).toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return List.of(Message.email(c.reservation().guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
