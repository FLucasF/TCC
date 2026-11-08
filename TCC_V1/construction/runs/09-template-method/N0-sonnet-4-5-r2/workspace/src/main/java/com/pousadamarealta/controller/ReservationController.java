package com.pousadamarealta.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.InvalidRequestException;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.util.DateUtils;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController() {
        this.reservationService = new ReservationService();
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            String roomNumber = getRequiredString(json, "roomNumber");
            String guestName = getRequiredString(json, "guestName");
            String guestEmail = getRequiredString(json, "guestEmail");
            String guestCpf = getRequiredString(json, "guestCpf");
            LocalDate checkIn = getRequiredDate(json, "checkIn");
            LocalDate checkOut = getRequiredDate(json, "checkOut");
            int guests = getRequiredInt(json, "guests");
            RatePlan ratePlan = getRequiredEnum(json, "ratePlan", RatePlan.class);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf,
                checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(toJson(reservation));
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid request");
        }
    }

    public void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toJson(reservation));
    }

    public void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toJson(reservation));
    }

    public void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toJson(reservation));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toJson(reservation));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(toDocument(reservation));
    }

    public void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toDocument(reservation));
    }

    private Map<String, Object> toJson(Reservation r) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", r.getId());
        json.put("roomNumber", r.getRoomNumber());
        json.put("guestName", r.getGuestName());
        json.put("guestEmail", r.getGuestEmail());
        json.put("guestCpf", r.getGuestCpf());
        json.put("checkIn", DateUtils.formatISO(r.getCheckIn()));
        json.put("checkOut", DateUtils.formatISO(r.getCheckOut()));
        json.put("guests", r.getGuests());
        json.put("ratePlan", r.getRatePlan().name());
        json.put("nights", r.getNights());
        json.put("subtotal", r.getSubtotal());
        json.put("discount", r.getDiscount());
        json.put("total", r.getTotal());
        json.put("status", r.getStatus().name());
        json.put("amountPaid", r.getAmountPaid());
        json.put("refund", r.getRefund());
        return json;
    }

    private Map<String, Object> toDocument(Reservation r) {
        Map<String, Object> json = new HashMap<>();
        json.put("title", "Comprovante de reserva");
        json.put("reservationId", r.getId());
        json.put("guestName", r.getGuestName());
        json.put("guestCpf", r.getGuestCpf());
        json.put("roomNumber", r.getRoomNumber());
        json.put("roomType", new com.pousadamarealta.repository.RoomRepository()
            .findByNumber(r.getRoomNumber()).get().getType().name());
        json.put("checkIn", DateUtils.formatISO(r.getCheckIn()));
        json.put("checkOut", DateUtils.formatISO(r.getCheckOut()));
        json.put("nights", r.getNights());
        json.put("subtotal", r.getSubtotal());
        json.put("discount", r.getDiscount());
        json.put("total", r.getTotal());
        return json;
    }

    private String getRequiredString(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new InvalidRequestException("Missing field: " + field);
        }
        return json.get(field).asText();
    }

    private int getRequiredInt(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new InvalidRequestException("Missing field: " + field);
        }
        return json.get(field).asInt();
    }

    private LocalDate getRequiredDate(JsonNode json, String field) {
        String dateStr = getRequiredString(json, field);
        try {
            return DateUtils.parseISO(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + field);
        }
    }

    private <T extends Enum<T>> T getRequiredEnum(JsonNode json, String field, Class<T> enumClass) {
        String value = getRequiredString(json, field);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid value for " + field);
        }
    }
}
