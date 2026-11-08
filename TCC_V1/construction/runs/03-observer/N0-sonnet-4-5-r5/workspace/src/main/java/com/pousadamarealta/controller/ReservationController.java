package com.pousadamarealta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.exception.AppException;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReservationController(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = getRequiredString(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = parseDate(getRequiredString(body, "checkIn"));
            LocalDate checkOut = parseDate(getRequiredString(body, "checkOut"));
            int guests = getRequiredInt(body, "guests");
            RatePlan ratePlan = RatePlan.valueOf(getRequiredString(body, "ratePlan"));

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(reservationToMap(reservation));
        } catch (IllegalArgumentException e) {
            throw AppException.invalidRequest("Dados inválidos: " + e.getMessage());
        }
    }

    public void getReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(reservationToMap(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(buildDocument(reservation));
    }

    public void getDocument(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);

        if (reservation.getAmountPaid().compareTo(BigDecimal.ZERO) == 0) {
            throw AppException.operationNotAllowed("Documento só disponível para reservas pagas");
        }

        ctx.json(buildDocument(reservation));
    }

    private Map<String, Object> reservationToMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", r.getCheckIn().format(dateFormatter));
        map.put("checkOut", r.getCheckOut().format(dateFormatter));
        map.put("guests", r.getGuests());
        map.put("ratePlan", r.getRatePlan().name());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        map.put("status", r.getStatus().name());
        map.put("amountPaid", r.getAmountPaid());
        map.put("refund", r.getRefund());
        return map;
    }

    private Map<String, Object> buildDocument(Reservation r) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("title", "Comprovante de reserva");
        doc.put("reservationId", r.getId());
        doc.put("guestName", r.getGuestName());
        doc.put("guestCpf", r.getGuestCpf());
        doc.put("roomNumber", r.getRoomNumber());
        doc.put("roomType", getRoomTypeName(r.getRoomNumber()));
        doc.put("checkIn", r.getCheckIn().format(displayDateFormatter));
        doc.put("checkOut", r.getCheckOut().format(displayDateFormatter));
        doc.put("nights", r.getNights());
        doc.put("subtotal", r.getSubtotal());
        doc.put("discount", r.getDiscount());
        doc.put("total", r.getTotal());
        return doc;
    }

    private String getRoomTypeName(String roomNumber) {
        if (roomNumber.startsWith("1")) return "STANDARD";
        if (roomNumber.startsWith("2")) return "DELUXE";
        return "SUITE";
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw AppException.invalidRequest("Campo obrigatório: " + key);
        }
        return value.toString();
    }

    private int getRequiredInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw AppException.invalidRequest("Campo obrigatório: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            throw AppException.invalidRequest("Valor inválido para " + key);
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, dateFormatter);
        } catch (DateTimeParseException e) {
            throw AppException.invalidRequest("Data inválida: " + date);
        }
    }
}
