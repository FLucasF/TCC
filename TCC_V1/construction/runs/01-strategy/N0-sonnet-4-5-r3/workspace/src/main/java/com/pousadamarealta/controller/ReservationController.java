package com.pousadamarealta.controller;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

        validateRequiredField(body, "roomNumber");
        validateRequiredField(body, "guestName");
        validateRequiredField(body, "guestEmail");
        validateRequiredField(body, "guestCpf");
        validateRequiredField(body, "checkIn");
        validateRequiredField(body, "checkOut");
        validateRequiredField(body, "guests");
        validateRequiredField(body, "ratePlan");

        Reservation reservation = new Reservation();
        reservation.setRoomNumber((String) body.get("roomNumber"));
        reservation.setGuestName((String) body.get("guestName"));
        reservation.setGuestEmail((String) body.get("guestEmail"));
        reservation.setGuestCpf((String) body.get("guestCpf"));

        try {
            reservation.setCheckIn(LocalDate.parse((String) body.get("checkIn"), DateTimeFormatter.ISO_LOCAL_DATE));
            reservation.setCheckOut(LocalDate.parse((String) body.get("checkOut"), DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format. Expected: yyyy-MM-dd");
        }

        if (!reservation.getCheckOut().isAfter(reservation.getCheckIn())) {
            throw new ApiException(400, "INVALID_REQUEST", "Check-out date must be after check-in date");
        }

        try {
            reservation.setGuests(((Number) body.get("guests")).intValue());
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid guests value");
        }

        try {
            reservation.setRatePlan(RatePlan.valueOf((String) body.get("ratePlan")));
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid rate plan");
        }

        if (body.containsKey("companyCode")) {
            reservation.setCompanyCode((String) body.get("companyCode"));
        }

        Reservation created = reservationService.createReservation(reservation);
        ctx.status(201);
        ctx.json(reservationToMap(created));
    }

    public void getReservation(Context ctx) {
        Long id = parseLongId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = parseLongId(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = parseLongId(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(reservationToMap(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = parseLongId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(reservationToMap(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = parseLongId(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(documentToMap(document));
    }

    public void getDocument(Context ctx) {
        Long id = parseLongId(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(documentToMap(document));
    }

    private void validateRequiredField(Map<String, Object> body, String field) {
        if (!body.containsKey(field) || body.get(field) == null) {
            throw new ApiException(400, "INVALID_REQUEST", "Missing required field: " + field);
        }
    }

    private Long parseLongId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    private Map<String, Object> reservationToMap(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", reservation.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE));
        map.put("checkOut", reservation.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE));
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan().name());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus().name());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> documentToMap(Document document) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", document.getTitle());
        map.put("reservationId", document.getReservationId());
        map.put("guestName", document.getGuestName());
        map.put("guestCpf", document.getGuestCpf());
        map.put("roomNumber", document.getRoomNumber());
        map.put("roomType", document.getRoomType().name());
        map.put("checkIn", document.getCheckIn().format(DateTimeFormatter.ISO_LOCAL_DATE));
        map.put("checkOut", document.getCheckOut().format(DateTimeFormatter.ISO_LOCAL_DATE));
        map.put("nights", document.getNights());
        map.put("subtotal", document.getSubtotal());
        map.put("discount", document.getDiscount());
        map.put("total", document.getTotal());
        return map;
    }
}
