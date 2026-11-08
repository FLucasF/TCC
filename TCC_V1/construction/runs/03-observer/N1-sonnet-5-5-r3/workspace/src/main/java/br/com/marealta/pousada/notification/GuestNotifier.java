package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.util.List;

public class GuestNotifier implements ReservationListener {

    @Override
    public List<Message> onConfirmed(Reservation reservation, Room room) {
        String body = "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(reservation, room).render() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return List.of(Message.email(reservation.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
