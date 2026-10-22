package pousada.document;

import java.util.Map;
import pousada.domain.Reservation;
import pousada.domain.Rooms;

/** O documento emitido para uma reserva paga. */
public record Document(DocumentType type, DocumentData data, Map<String, String> identity) {

    public static Document issue(Reservation r) {
        var room = Rooms.find(r.roomNumber()).orElseThrow();
        var data = new DocumentData(r.id(), r.guestName(), r.roomNumber(), room.type(), r.checkIn(),
                r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
        return new Document(DocumentTypes.byCode(r.identity().type()), data, r.identity().values());
    }

    public String text() {
        return type.render(data, identity);
    }
}
