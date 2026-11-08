package br.com.marealta.http;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;

/** Como o domínio aparece no JSON combinado com o site. */
final class Json {
    private Json() {
    }

    static Map<String, Object> room(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type().name());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    static Map<String, Object> reservation(Reservation reservation) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", reservation.id());
        json.put("roomNumber", reservation.roomNumber());
        json.put("guestName", reservation.guestName());
        json.put("guestEmail", reservation.guestEmail());
        json.put("guestCpf", reservation.guestCpf());
        json.put("checkIn", reservation.checkIn().toString());
        json.put("checkOut", reservation.checkOut().toString());
        json.put("guests", reservation.guests());
        json.put("ratePlan", reservation.ratePlan());
        json.put("nights", reservation.nights());
        json.put("subtotal", reservation.subtotal());
        json.put("discount", reservation.discount());
        json.put("total", reservation.total());
        json.put("status", reservation.status().name());
        json.put("amountPaid", reservation.amountPaid());
        json.put("refund", reservation.refund());
        return json;
    }

    static Map<String, Object> receipt(Receipt receipt) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", Receipt.TITLE);
        json.put("reservationId", receipt.reservationId());
        json.put("guestName", receipt.guestName());
        json.put("guestCpf", receipt.guestCpf());
        json.put("roomNumber", receipt.roomNumber());
        json.put("roomType", receipt.roomType().name());
        json.put("checkIn", receipt.checkIn().toString());
        json.put("checkOut", receipt.checkOut().toString());
        json.put("nights", receipt.nights());
        json.put("subtotal", receipt.subtotal());
        json.put("discount", receipt.discount());
        json.put("total", receipt.total());
        return json;
    }

    static Map<String, Object> message(Message message) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", message.id());
        json.put("channel", message.channel());
        json.put("to", message.to());
        json.put("subject", message.subject());
        json.put("body", message.body());
        return json;
    }

    static Map<String, Object> error(String code, String message) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("error", code);
        json.put("message", message);
        return json;
    }
}
