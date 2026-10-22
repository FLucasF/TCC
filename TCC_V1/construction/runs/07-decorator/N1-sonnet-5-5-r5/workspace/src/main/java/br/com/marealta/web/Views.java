package br.com.marealta.web;

import br.com.marealta.message.Message;
import br.com.marealta.reservation.Receipt;
import br.com.marealta.reservation.Reservation;
import br.com.marealta.reservation.Room;

import java.util.LinkedHashMap;
import java.util.Map;

/** Como cada objeto aparece no JSON combinado com o site. */
public final class Views {

    private Views() {
    }

    public static Map<String, Object> room(Room room) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("number", room.number());
        view.put("type", room.type());
        view.put("capacity", room.capacity());
        view.put("nightlyRate", room.nightlyRate());
        return view;
    }

    public static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", r.id());
        view.put("roomNumber", r.roomNumber());
        view.put("guestName", r.guestName());
        view.put("guestEmail", r.guestEmail());
        view.put("guestCpf", r.guestCpf());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("guests", r.guests());
        view.put("ratePlan", r.ratePlan().name());
        view.put("nights", r.nights());
        view.put("subtotal", r.subtotal());
        view.put("discount", r.discount());
        view.put("total", r.total());
        view.put("status", r.status().name());
        view.put("amountPaid", r.amountPaid());
        view.put("refund", r.refund());
        return view;
    }

    public static Map<String, Object> receipt(Receipt r) {
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

    public static Map<String, Object> message(Message m) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", m.id());
        view.put("channel", m.channel().name());
        view.put("to", m.to());
        view.put("subject", m.subject());
        view.put("body", m.body());
        view.put("status", m.status().name());
        return view;
    }
}
