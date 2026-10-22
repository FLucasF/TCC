package pousada.web;

import java.util.LinkedHashMap;
import java.util.Map;
import pousada.document.Document;
import pousada.document.DocumentTypes;
import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.Room;

/** Como cada objeto aparece no JSON. */
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
        putIdentity(view, r.identity().values());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("guests", r.guests());
        view.put("ratePlan", r.ratePlan());
        view.put("nights", r.nights());
        view.put("subtotal", r.subtotal());
        view.put("discount", r.discount());
        view.put("total", r.total());
        view.put("status", r.status());
        view.put("amountPaid", r.amountPaid());
        view.put("refund", r.refund());
        return view;
    }

    static Map<String, Object> document(Document d) {
        var data = d.data();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", d.type().title());
        view.put("reservationId", data.reservationId());
        view.put("guestName", data.guestName());
        putIdentity(view, d.identity());
        view.put("roomNumber", data.roomNumber());
        view.put("roomType", data.roomType());
        view.put("checkIn", data.checkIn().toString());
        view.put("checkOut", data.checkOut().toString());
        view.put("nights", data.nights());
        view.put("subtotal", data.subtotal());
        view.put("discount", data.discount());
        view.put("total", data.total());
        view.put("text", d.text());
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

    /** Todos os campos de identificação aparecem; os que não se aplicam vão null. */
    private static void putIdentity(Map<String, Object> view, Map<String, String> values) {
        for (String field : DocumentTypes.allFields()) {
            view.put(field, values.get(field));
        }
    }
}
