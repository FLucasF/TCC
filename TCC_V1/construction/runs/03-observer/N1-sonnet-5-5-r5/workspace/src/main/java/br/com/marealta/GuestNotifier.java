package br.com.marealta;

import java.util.List;

/** Avisa o hóspede, no e-mail dele. */
public class GuestNotifier implements ReservationListener {
    private final Rooms rooms;

    public GuestNotifier(Rooms rooms) {
        this.rooms = rooms;
    }

    @Override
    public List<Message> confirmed(Reservation r) {
        String receipt = Receipt.of(r, rooms.get(r.roomNumber())).toText();
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return List.of(Message.email(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
