package br.com.mare.web;

import br.com.mare.domain.Message;
import br.com.mare.domain.Receipt;
import br.com.mare.domain.Reservation;
import br.com.mare.domain.Room;

import java.util.LinkedHashMap;
import java.util.Map;

/** Formato JSON combinado com o site. */
final class Views {
    private Views() {}

    static Map<String, Object> room(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
        return m;
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.room().number());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.put("guestCpf", r.guestCpf());
        m.put("checkIn", r.checkInDate().toString());
        m.put("checkOut", r.checkOutDate().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan().code());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    static Map<String, Object> receipt(Receipt r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", r.title());
        m.put("reservationId", r.reservationId());
        m.put("guestName", r.guestName());
        m.put("guestCpf", r.guestCpf());
        m.put("roomNumber", r.roomNumber());
        m.put("roomType", r.roomType());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        return m;
    }

    static Map<String, Object> message(Message m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.id());
        v.put("channel", m.channel());
        v.put("to", m.to());
        v.put("subject", m.subject());
        v.put("body", m.body());
        return v;
    }
}
