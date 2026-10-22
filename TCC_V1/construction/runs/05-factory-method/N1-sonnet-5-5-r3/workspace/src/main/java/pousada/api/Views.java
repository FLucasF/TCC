package pousada.api;

import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.identity.IdentityKind;
import pousada.identity.ReservationDocument;

import java.util.LinkedHashMap;
import java.util.Map;

/** Como cada objeto do domínio aparece no JSON. */
final class Views {

    private Views() {
    }

    static Map<String, Object> room(Room room) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("number", room.number());
        view.put("type", room.type());
        view.put("capacity", room.capacity());
        view.put("nightlyRate", room.nightlyRate());
        return view;
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", r.id());
        view.put("roomNumber", r.room().number());
        view.put("guestName", r.guestName());
        view.put("guestEmail", r.guestEmail());
        view.putAll(IdentityKind.fieldsOf(r.identity()));
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("guests", r.guests());
        view.put("ratePlan", r.ratePlan());
        view.put("nights", r.quote().nights());
        view.put("subtotal", r.quote().subtotal());
        view.put("discount", r.quote().discount());
        view.put("total", r.quote().total());
        view.put("status", r.status());
        view.put("amountPaid", r.amountPaid());
        view.put("refund", r.refund());
        return view;
    }

    static Map<String, Object> document(Reservation r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", ReservationDocument.title(r));
        view.put("reservationId", r.id());
        view.put("guestName", r.guestName());
        view.putAll(IdentityKind.fieldsOf(r.identity()));
        view.put("roomNumber", r.room().number());
        view.put("roomType", r.room().type());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("nights", r.quote().nights());
        view.put("subtotal", r.quote().subtotal());
        view.put("discount", r.quote().discount());
        view.put("total", r.quote().total());
        view.put("text", ReservationDocument.text(r));
        return view;
    }

    static Map<String, Object> message(Message m) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", m.id());
        view.put("channel", m.channel());
        view.put("to", m.to());
        view.put("subject", m.subject());
        view.put("body", m.body());
        return view;
    }
}
