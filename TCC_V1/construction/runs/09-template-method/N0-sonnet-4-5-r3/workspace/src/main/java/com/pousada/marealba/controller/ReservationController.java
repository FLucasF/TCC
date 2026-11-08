package com.pousada.marealba.controller;

import com.pousada.marealba.exception.ApiException;
import com.pousada.marealba.model.*;
import com.pousada.marealba.service.ReservationService;
import com.pousada.marealba.util.DateUtil;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void createReservation(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);

        try {
            Integer roomNumber = getRequiredInteger(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = getRequiredDate(body, "checkIn");
            LocalDate checkOut = getRequiredDate(body, "checkOut");
            Integer guests = getRequiredInteger(body, "guests");
            RatePlan ratePlan = getRequiredEnum(body, "ratePlan", RatePlan.class);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toReservationMap(reservation));

        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest(e.getMessage());
        }
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationMap(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationMap(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationMap(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationMap(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Document document = reservationService.checkOut(id);
        ctx.json(toDocumentMap(document));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentMap(document));
    }

    private Map<String, Object> toReservationMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", DateUtil.formatIso(r.getCheckIn()));
        map.put("checkOut", DateUtil.formatIso(r.getCheckOut()));
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

    private Map<String, Object> toDocumentMap(Document d) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", d.getTitle());
        map.put("reservationId", d.getReservationId());
        map.put("guestName", d.getGuestName());
        map.put("guestCpf", d.getGuestCpf());
        map.put("roomNumber", d.getRoomNumber());
        map.put("roomType", d.getRoomType().name());
        map.put("checkIn", DateUtil.formatIso(d.getCheckIn()));
        map.put("checkOut", DateUtil.formatIso(d.getCheckOut()));
        map.put("nights", d.getNights());
        map.put("subtotal", d.getSubtotal());
        map.put("discount", d.getDiscount());
        map.put("total", d.getTotal());
        return map;
    }

    private String getRequiredString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value.toString();
    }

    private Integer getRequiredInteger(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("Invalid integer value for field: " + key);
    }

    private LocalDate getRequiredDate(Map<String, Object> body, String key) {
        String value = getRequiredString(body, key);
        try {
            return DateUtil.parseIso(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for field: " + key);
        }
    }

    private <T extends Enum<T>> T getRequiredEnum(Map<String, Object> body, String key, Class<T> enumClass) {
        String value = getRequiredString(body, key);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for field: " + key);
        }
    }
}
