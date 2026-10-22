package br.com.marealta.web;

import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.messaging.Message;

import java.util.LinkedHashMap;
import java.util.Map;

/** O formato JSON combinado com o site. */
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
        m.put("roomNumber", r.roomNumber());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.put("guestCpf", r.guestCpf());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan());
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

    static Map<String, Object> message(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        m.put("status", msg.status());
        return m;
    }
}
