package br.com.marealta.web;

import br.com.marealta.domain.Document;
import br.com.marealta.domain.IdentityKinds;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

import java.util.LinkedHashMap;
import java.util.Map;

/** Formato JSON combinado com o site. */
final class Views {
    private Views() {
    }

    static Map<String, Object> room(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.type().capacity());
        m.put("nightlyRate", r.type().nightlyRate());
        return m;
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.room().number());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        identity(m, r.identity().fields());
        m.put("checkIn", r.checkInDate().toString());
        m.put("checkOut", r.checkOutDate().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan());
        m.put("nights", r.quote().nights());
        m.put("subtotal", r.quote().subtotal());
        m.put("discount", r.quote().discount());
        m.put("total", r.quote().total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    static Map<String, Object> document(Document d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", d.title());
        m.put("reservationId", d.data().reservationId());
        m.put("guestName", d.data().guestName());
        identity(m, d.identityFields());
        m.put("roomNumber", d.data().roomNumber());
        m.put("roomType", d.data().roomType());
        m.put("checkIn", d.data().checkIn().toString());
        m.put("checkOut", d.data().checkOut().toString());
        m.put("nights", d.data().nights());
        m.put("subtotal", d.data().subtotal());
        m.put("discount", d.data().discount());
        m.put("total", d.data().total());
        m.put("text", d.text());
        return m;
    }

    static Map<String, Object> message(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        return m;
    }

    /** Todos os campos de identificação aparecem; os que não se aplicam vão null. */
    private static void identity(Map<String, Object> target, Map<String, String> fields) {
        for (String f : IdentityKinds.allFields()) {
            target.put(f, fields.get(f));
        }
    }
}
