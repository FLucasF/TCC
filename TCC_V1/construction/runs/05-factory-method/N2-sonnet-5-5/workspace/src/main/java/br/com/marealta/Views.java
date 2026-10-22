package br.com.marealta;

import java.util.LinkedHashMap;
import java.util.Map;

/** Como reservas, documentos, mensagens e quartos aparecem no JSON. */
public class Views {
    private final IdentityTypes identityTypes;

    public Views(IdentityTypes identityTypes) {
        this.identityTypes = identityTypes;
    }

    public Map<String, Object> room(Room room) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", room.number());
        m.put("type", room.type());
        m.put("capacity", room.type().capacity());
        m.put("nightlyRate", Money.round(room.type().nightlyRate()));
        return m;
    }

    public Map<String, Object> reservation(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.room().number());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        putIdentity(m, r);
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan());
        m.put("nights", r.price().nights());
        m.put("subtotal", r.price().subtotal());
        m.put("discount", r.price().discount());
        m.put("total", r.price().total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    public Map<String, Object> document(Reservation r) {
        DocumentFormat format = r.identity().format();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", format.title());
        m.put("reservationId", r.id());
        m.put("guestName", r.guestName());
        putIdentity(m, r);
        m.put("roomNumber", r.room().number());
        m.put("roomType", r.room().type());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.price().nights());
        m.put("subtotal", r.price().subtotal());
        m.put("discount", r.price().discount());
        m.put("total", r.price().total());
        m.put("text", format.render(r));
        return m;
    }

    public Map<String, Object> message(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        return m;
    }

    private void putIdentity(Map<String, Object> m, Reservation r) {
        for (String field : identityTypes.allFields()) {
            m.put(field, r.identity().get(field));
        }
    }
}
