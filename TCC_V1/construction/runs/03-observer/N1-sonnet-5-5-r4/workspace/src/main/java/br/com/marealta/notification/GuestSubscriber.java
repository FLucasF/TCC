package br.com.marealta.notification;

import br.com.marealta.app.Notification;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.RoomCatalog;

import java.util.List;

/** O hóspede recebe o comprovante quando a reserva é confirmada. */
public class GuestSubscriber implements Subscriber {
    private final RoomCatalog rooms;

    public GuestSubscriber(RoomCatalog rooms) {
        this.rooms = rooms;
    }

    @Override
    public List<Notification> onConfirmed(ReservationEvent.Confirmed event) {
        Reservation r = event.reservation();
        Receipt receipt = Receipt.of(r, rooms.require(r.roomNumber()));
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt.toText() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        return List.of(Notification.email(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
    }
}
