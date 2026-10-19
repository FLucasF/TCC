package br.com.mareaalta.api;

import br.com.mareaalta.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ApiMapper {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public static Map<String, Object> toRoomResponse(Room room) {
        Map<String, Object> response = new HashMap<>();
        response.put("number", room.getNumber());
        response.put("type", room.getType().toString());
        response.put("capacity", room.getCapacity());
        response.put("nightlyRate", room.getNightlyRate());
        return response;
    }

    public static Map<String, Object> toReservationResponse(Reservation reservation) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", reservation.getId());
        response.put("roomNumber", reservation.getRoomNumber());
        response.put("guestName", reservation.getGuestName());
        response.put("guestEmail", reservation.getGuestEmail());
        response.put("guestCpf", reservation.getGuestCpf());
        response.put("companyCnpj", reservation.getCompanyCnpj());
        response.put("companyName", reservation.getCompanyName());
        response.put("guestPassport", reservation.getGuestPassport());
        response.put("guestCountry", reservation.getGuestCountry());
        response.put("checkIn", reservation.getCheckIn().format(DATE_FORMAT));
        response.put("checkOut", reservation.getCheckOut().format(DATE_FORMAT));
        response.put("guests", reservation.getGuests());
        response.put("ratePlan", reservation.getRatePlan().toString());
        response.put("nights", reservation.getNights());
        response.put("subtotal", reservation.getSubtotal());
        response.put("discount", reservation.getDiscount());
        response.put("total", reservation.getTotal());
        response.put("status", reservation.getStatus().toString());
        response.put("amountPaid", reservation.getAmountPaid());
        response.put("refund", reservation.getRefund());
        return response;
    }

    public static Map<String, Object> toDocumentResponse(DocumentData document) {
        Map<String, Object> response = new HashMap<>();
        response.put("title", document.getTitle());
        response.put("reservationId", document.getReservationId());
        response.put("guestName", document.getGuestName());
        response.put("guestCpf", document.getGuestCpf());
        response.put("companyCnpj", document.getCompanyCnpj());
        response.put("companyName", document.getCompanyName());
        response.put("guestPassport", document.getGuestPassport());
        response.put("guestCountry", document.getGuestCountry());
        response.put("roomNumber", document.getRoomNumber());
        response.put("roomType", document.getRoomType() != null ? document.getRoomType().toString() : null);
        response.put("checkIn", document.getCheckIn());
        response.put("checkOut", document.getCheckOut());
        response.put("nights", document.getNights());
        response.put("subtotal", document.getSubtotal());
        response.put("discount", document.getDiscount());
        response.put("total", document.getTotal());
        response.put("text", document.getText());
        return response;
    }

    public static Map<String, Object> toMessageResponse(Message message) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        response.put("channel", message.getChannel());
        response.put("to", message.getTo());
        response.put("subject", message.getSubject());
        response.put("body", message.getBody());
        return response;
    }

    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    public static YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr, MONTH_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format. Expected: yyyy-MM");
        }
    }

    public static RatePlan parseRatePlan(String ratePlanStr) {
        try {
            return RatePlan.valueOf(ratePlanStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid rate plan: " + ratePlanStr);
        }
    }
}
