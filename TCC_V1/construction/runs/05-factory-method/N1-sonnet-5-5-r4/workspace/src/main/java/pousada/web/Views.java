package pousada.web;

import pousada.document.IssuedDocument;
import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.identity.IdentityKinds;

import java.util.LinkedHashMap;
import java.util.Map;

/** Monta o JSON combinado com o site. */
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
        view.put("roomNumber", r.roomNumber());
        view.put("guestName", r.guestName());
        view.put("guestEmail", r.guestEmail());
        putIdentity(view, r);
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

    static Map<String, Object> document(Reservation r, IssuedDocument document) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", document.title());
        view.put("reservationId", r.id());
        view.put("guestName", r.guestName());
        putIdentity(view, r);
        view.put("roomNumber", r.roomNumber());
        view.put("roomType", pousada.domain.RoomCatalog.require(r.roomNumber()).type());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("nights", r.quote().nights());
        view.put("subtotal", r.quote().subtotal());
        view.put("discount", r.quote().discount());
        view.put("total", r.quote().total());
        view.put("text", document.text());
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

    private static void putIdentity(Map<String, Object> view, Reservation r) {
        Map<String, String> fields = r.identity().fields();
        for (String name : IdentityKinds.allFieldNames()) {
            view.put(name, fields.get(name));
        }
    }
}
