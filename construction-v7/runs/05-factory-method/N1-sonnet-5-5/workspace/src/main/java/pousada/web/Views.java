package pousada.web;

import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.document.Document;
import pousada.domain.document.Voucher;
import pousada.domain.identification.IdentificationKinds;

import java.util.LinkedHashMap;
import java.util.Map;

/** Formato JSON de cada recurso, como o site espera. */
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
        view.putAll(IdentificationKinds.withNulls(r.identification()));
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

    static Map<String, Object> document(Document d) {
        Voucher v = d.voucher();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", d.title());
        view.put("reservationId", v.reservationId());
        view.put("guestName", v.guestName());
        view.putAll(d.identification());
        view.put("roomNumber", v.roomNumber());
        view.put("roomType", v.roomType());
        view.put("checkIn", v.checkIn().toString());
        view.put("checkOut", v.checkOut().toString());
        view.put("nights", v.nights());
        view.put("subtotal", v.subtotal());
        view.put("discount", v.discount());
        view.put("total", v.total());
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
}
