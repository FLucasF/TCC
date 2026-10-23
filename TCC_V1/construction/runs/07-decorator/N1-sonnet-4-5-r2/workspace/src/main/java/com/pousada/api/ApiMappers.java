package com.pousada.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pousada.domain.Message;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

import java.time.format.DateTimeFormatter;

public class ApiMappers {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode toRoomJson(Room room) {
        ObjectNode json = mapper.createObjectNode();
        json.put("number", room.getNumber());
        json.put("type", room.getType().name());
        json.put("capacity", room.getCapacity());
        json.put("nightlyRate", room.getNightlyRate());
        return json;
    }

    public static JsonNode toReservationJson(Reservation reservation) {
        ObjectNode json = mapper.createObjectNode();
        json.put("id", reservation.getId());
        json.put("roomNumber", reservation.getRoomNumber());
        json.put("guestName", reservation.getGuestName());
        json.put("guestEmail", reservation.getGuestEmail());
        json.put("guestCpf", reservation.getGuestCpf());
        json.put("checkIn", reservation.getCheckIn().format(DATE_FORMAT));
        json.put("checkOut", reservation.getCheckOut().format(DATE_FORMAT));
        json.put("guests", reservation.getGuests());
        json.put("ratePlan", reservation.getRatePlan().name());
        json.put("nights", reservation.getNights());
        json.put("subtotal", reservation.getSubtotal());
        json.put("discount", reservation.getDiscount());
        json.put("total", reservation.getTotal());
        json.put("status", reservation.getStatus().name());
        json.put("amountPaid", reservation.getAmountPaid());
        if (reservation.getRefund() != null) {
            json.put("refund", reservation.getRefund());
        } else {
            json.putNull("refund");
        }
        return json;
    }

    public static JsonNode toMessageJson(Message message) {
        ObjectNode json = mapper.createObjectNode();
        json.put("id", message.getId());
        json.put("channel", message.getChannel().name());
        json.put("to", message.getTo());
        json.put("subject", message.getSubject());
        json.put("body", message.getBody());
        json.put("status", message.getStatus().name());
        return json;
    }

    public static JsonNode toDocumentJson(Reservation reservation, String roomType, String documentText) {
        ObjectNode json = mapper.createObjectNode();
        json.put("title", "Comprovante de reserva");
        json.put("reservationId", reservation.getId());
        json.put("guestName", reservation.getGuestName());
        json.put("guestCpf", reservation.getGuestCpf());
        json.put("roomNumber", reservation.getRoomNumber());
        json.put("roomType", roomType);
        json.put("checkIn", reservation.getCheckIn().format(DATE_FORMAT));
        json.put("checkOut", reservation.getCheckOut().format(DATE_FORMAT));
        json.put("nights", reservation.getNights());
        json.put("subtotal", reservation.getSubtotal());
        json.put("discount", reservation.getDiscount());
        json.put("total", reservation.getTotal());
        return json;
    }
}
