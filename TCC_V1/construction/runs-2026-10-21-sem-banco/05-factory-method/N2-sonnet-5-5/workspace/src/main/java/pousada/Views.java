package pousada;

import java.util.LinkedHashMap;
import java.util.Map;

/** Formato JSON combinado com o site. */
public class Views {
    private final IdentityRegistry identities;

    public Views(IdentityRegistry identities) { this.identities = identities; }

    public Map<String, Object> room(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    public Map<String, Object> reservation(Reservation r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.id());
        json.put("roomNumber", r.room().number());
        json.put("guestName", r.guestName());
        json.put("guestEmail", r.guestEmail());
        json.putAll(identityFields(r.identity()));
        json.put("checkIn", r.checkInDate().toString());
        json.put("checkOut", r.checkOutDate().toString());
        json.put("guests", r.guests());
        json.put("ratePlan", r.ratePlan());
        json.put("nights", r.quote().nights());
        json.put("subtotal", r.quote().subtotal());
        json.put("discount", r.quote().discount());
        json.put("total", r.quote().total());
        json.put("status", r.getStatus());
        json.put("amountPaid", r.amountPaid());
        json.put("refund", r.refund());
        return json;
    }

    public Map<String, Object> document(ReservationDocument d) {
        DocumentData data = d.data();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", d.titleText());
        json.put("reservationId", data.reservationId());
        json.put("guestName", data.guestName());
        json.putAll(identityFields(d.identity()));
        json.put("roomNumber", data.roomNumber());
        json.put("roomType", data.roomType());
        json.put("checkIn", data.checkIn().toString());
        json.put("checkOut", data.checkOut().toString());
        json.put("nights", data.quote().nights());
        json.put("subtotal", data.quote().subtotal());
        json.put("discount", data.quote().discount());
        json.put("total", data.quote().total());
        json.put("text", d.text());
        return json;
    }

    public Map<String, Object> message(Message m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", m.id());
        json.put("channel", m.channel());
        json.put("to", m.to());
        json.put("subject", m.subject());
        json.put("body", m.body());
        return json;
    }

    /** Todos os campos de identificação; os que não se aplicam vão null. */
    private Map<String, Object> identityFields(GuestIdentity identity) {
        Map<String, Object> json = new LinkedHashMap<>();
        for (String key : identities.allKeys()) {
            json.put(key, identity.fields().get(key));
        }
        return json;
    }
}
