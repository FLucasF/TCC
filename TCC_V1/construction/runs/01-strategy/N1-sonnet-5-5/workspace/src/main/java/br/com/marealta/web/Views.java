package br.com.marealta.web;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.service.Receipt;
import java.util.LinkedHashMap;
import java.util.Map;

/** Como cada objeto aparece no JSON combinado com o site. */
final class Views {

    private Views() {
    }

    static Map<String, Object> room(Room room) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("number", room.number());
        view.put("type", room.type().name());
        view.put("capacity", room.capacity());
        view.put("nightlyRate", room.type().nightlyRate());
        return view;
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", r.id());
        view.put("roomNumber", r.roomNumber());
        view.put("guestName", r.guestName());
        view.put("guestEmail", r.guestEmail());
        view.put("guestCpf", r.guestCpf());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("guests", r.guests());
        view.put("ratePlan", r.ratePlan());
        view.put("nights", r.nights());
        view.put("subtotal", r.subtotal());
        view.put("discount", r.discount());
        view.put("total", r.total());
        view.put("status", r.status().name());
        view.put("amountPaid", r.amountPaid());
        view.put("refund", r.refund());
        return view;
    }

    static Map<String, Object> receipt(Receipt r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", Receipt.TITLE);
        view.put("reservationId", r.reservationId());
        view.put("guestName", r.guestName());
        view.put("guestCpf", r.guestCpf());
        view.put("roomNumber", r.roomNumber());
        view.put("roomType", r.roomType());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("nights", r.nights());
        view.put("subtotal", r.subtotal());
        view.put("discount", r.discount());
        view.put("total", r.total());
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
