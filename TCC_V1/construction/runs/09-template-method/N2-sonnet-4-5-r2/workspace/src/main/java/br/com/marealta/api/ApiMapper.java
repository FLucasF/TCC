package br.com.marealta.api;

import br.com.marealta.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ApiMapper {
    private final ObjectMapper objectMapper;

    public ApiMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode mapRoom(Room room) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("number", room.getNumber());
        node.put("type", room.getType().name());
        node.put("capacity", room.getCapacity());
        node.put("nightlyRate", room.getNightlyRate());
        return node;
    }

    public ObjectNode mapReservation(Reservation reservation) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", reservation.getId());
        node.put("roomNumber", reservation.getRoomNumber());
        node.put("guestName", reservation.getGuestName());
        node.put("guestEmail", reservation.getGuestEmail());
        node.put("guestCpf", reservation.getGuestCpf());
        node.put("checkIn", reservation.getCheckIn().toString());
        node.put("checkOut", reservation.getCheckOut().toString());
        node.put("guests", reservation.getGuests());
        node.put("ratePlan", reservation.getRatePlan().name());
        node.put("nights", reservation.getNights());
        node.put("subtotal", reservation.getSubtotal());
        node.put("discount", reservation.getDiscount());
        node.put("total", reservation.getTotal());
        node.put("status", reservation.getStatus().name());
        node.put("amountPaid", reservation.getAmountPaid());
        if (reservation.getRefund() != null) {
            node.put("refund", reservation.getRefund());
        } else {
            node.putNull("refund");
        }
        return node;
    }

    public ObjectNode mapDocument(Document document) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("title", document.getTitle());
        node.put("reservationId", document.getReservationId());
        node.put("guestName", document.getGuestName());
        node.put("guestCpf", document.getGuestCpf());
        node.put("roomNumber", document.getRoomNumber());
        node.put("roomType", document.getRoomType().name());
        node.put("checkIn", document.getCheckIn().toString());
        node.put("checkOut", document.getCheckOut().toString());
        node.put("nights", document.getNights());
        node.put("subtotal", document.getSubtotal());
        node.put("discount", document.getDiscount());
        node.put("total", document.getTotal());
        return node;
    }

    public ObjectNode mapMessage(Message message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", message.getId());
        node.put("channel", message.getChannel());
        node.put("to", message.getTo());
        node.put("subject", message.getSubject());
        node.put("body", message.getBody());
        return node;
    }

    public CreateReservationRequest parseCreateReservationRequest(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);

            int roomNumber = json.get("roomNumber").asInt();
            String guestName = json.get("guestName").asText();
            String guestEmail = json.get("guestEmail").asText();
            String guestCpf = json.get("guestCpf").asText();
            LocalDate checkIn = LocalDate.parse(json.get("checkIn").asText());
            LocalDate checkOut = LocalDate.parse(json.get("checkOut").asText());
            int guests = json.get("guests").asInt();
            RatePlan ratePlan = RatePlan.valueOf(json.get("ratePlan").asText());

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                throw new InvalidRequestException("Check-out must be after check-in");
            }

            if (guests < 1) {
                throw new InvalidRequestException("At least one guest is required");
            }

            return new CreateReservationRequest(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan
            );
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid rate plan");
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid request body");
        }
    }

    public LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }
    }

    public YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }

    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public record CreateReservationRequest(
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan
    ) {
    }
}
