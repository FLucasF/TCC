package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Receipt;

import java.util.List;

/** Avisa o hóspede, no e-mail dele, que a reserva foi confirmada. */
public class GuestNotifier implements Notifier {

    @Override
    public List<Message> messagesFor(ReservationEvent event) {
        if (!(event instanceof ReservationEvent.Confirmed confirmed)) {
            return List.of();
        }
        var reservation = confirmed.reservation();
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(reservation, confirmed.room()).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return List.of(Message.email(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
