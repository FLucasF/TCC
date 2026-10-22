package br.com.marealta.web;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.identity.DocumentData;
import br.com.marealta.identity.DocumentRenderer;
import br.com.marealta.identity.IdentityKinds;
import br.com.marealta.persistence.MessageRepository.Message;

import java.util.LinkedHashMap;
import java.util.Map;

/** Como cada objeto do sistema aparece no JSON. */
final class Views {
    private Views() {}

    static Map<String, Object> room(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.id());
        json.put("roomNumber", r.roomNumber());
        json.put("guestName", r.guestName());
        json.put("guestEmail", r.guestEmail());
        json.putAll(IdentityKinds.describe(r.identity()));
        json.put("checkIn", r.checkIn().toString());
        json.put("checkOut", r.checkOut().toString());
        json.put("guests", r.guests());
        json.put("ratePlan", r.ratePlan().name());
        json.put("nights", r.quote().nights());
        json.put("subtotal", r.quote().subtotal());
        json.put("discount", r.quote().discount());
        json.put("total", r.quote().total());
        json.put("status", r.status().name());
        json.put("amountPaid", r.amountPaid());
        json.put("refund", r.refund());
        return json;
    }

    static Map<String, Object> document(DocumentData d) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", d.identity().kind().documentTitle());
        json.put("reservationId", d.reservationId());
        json.put("guestName", d.guestName());
        json.putAll(IdentityKinds.describe(d.identity()));
        json.put("roomNumber", d.roomNumber());
        json.put("roomType", d.roomType());
        json.put("checkIn", d.checkIn().toString());
        json.put("checkOut", d.checkOut().toString());
        json.put("nights", d.quote().nights());
        json.put("subtotal", d.quote().subtotal());
        json.put("discount", d.quote().discount());
        json.put("total", d.quote().total());
        json.put("text", DocumentRenderer.render(d));
        return json;
    }

    static Map<String, Object> message(Message m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", m.id());
        json.put("channel", m.channel());
        json.put("to", m.to());
        json.put("subject", m.subject());
        json.put("body", m.body());
        return json;
    }
}
