package com.pousadamareal.api;

import com.pousadamareal.domain.Reservation;
import com.pousadamareal.domain.ReservationRequest;
import com.pousadamareal.service.DataStore;
import com.pousadamareal.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class ReservationController {
    private final ReservationService reservationService;
    private final DataStore dataStore;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public ReservationController(ReservationService reservationService, DataStore dataStore) {
        this.reservationService = reservationService;
        this.dataStore = dataStore;
    }

    public void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            int roomNumber = getIntField(body, "roomNumber");
            String guestName = getStringField(body, "guestName");
            String guestEmail = getStringField(body, "guestEmail");
            String guestCpf = getStringField(body, "guestCpf");
            String checkInStr = getStringField(body, "checkIn");
            String checkOutStr = getStringField(body, "checkOut");
            int guests = getIntField(body, "guests");
            String ratePlan = getStringField(body, "ratePlan");
            String companyCode = body.containsKey("companyCode") ? (String) body.get("companyCode") : null;

            LocalDate checkIn = parseDate(checkInStr);
            LocalDate checkOut = parseDate(checkOutStr);

            ReservationRequest request = new ReservationRequest(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan, companyCode
            );

            ReservationService.Result<Reservation> result = reservationService.createReservation(request);

            if (result.success()) {
                ctx.status(201);
                ctx.json(toJson(result.data()));
            } else {
                ctx.status(getStatusCodeForError(result.errorCode()));
                Map<String, String> error = new HashMap<>();
                error.put("error", result.errorCode());
                error.put("message", result.errorMessage());
                ctx.json(error);
            }
        } catch (Exception e) {
            sendBadRequest(ctx, "INVALID_REQUEST", "Invalid request body");
        }
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = dataStore.getReservation(id);

        if (reservation == null) {
            ctx.status(404);
            Map<String, String> error = new HashMap<>();
            error.put("error", "RESERVATION_NOT_FOUND");
            error.put("message", "Reservation not found");
            ctx.json(error);
        } else {
            ctx.json(toJson(reservation));
        }
    }

    public void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Object amountObj = body.get("amountPaid");
            double amount = (amountObj instanceof Number) ? ((Number) amountObj).doubleValue() : Double.parseDouble(amountObj.toString());
            long amountPaid = Math.round(amount * 100);

            ReservationService.Result<Reservation> result = reservationService.confirmReservation(id, amountPaid);

            if (result.success()) {
                ctx.json(toJson(result.data()));
            } else {
                ctx.status(getStatusCodeForError(result.errorCode()));
                Map<String, String> error = new HashMap<>();
                error.put("error", result.errorCode());
                error.put("message", result.errorMessage());
                ctx.json(error);
            }
        } catch (Exception e) {
            sendBadRequest(ctx, "INVALID_REQUEST", "Invalid request body");
        }
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReservationService.Result<Reservation> result = reservationService.cancelReservation(id);

        if (result.success()) {
            ctx.json(toJson(result.data()));
        } else {
            ctx.status(getStatusCodeForError(result.errorCode()));
            Map<String, String> error = new HashMap<>();
            error.put("error", result.errorCode());
            error.put("message", result.errorMessage());
            ctx.json(error);
        }
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReservationService.Result<Reservation> result = reservationService.checkIn(id);

        if (result.success()) {
            ctx.json(toJson(result.data()));
        } else {
            ctx.status(getStatusCodeForError(result.errorCode()));
            Map<String, String> error = new HashMap<>();
            error.put("error", result.errorCode());
            error.put("message", result.errorMessage());
            ctx.json(error);
        }
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReservationService.Result<ReservationService.ReservationReceipt> result = reservationService.checkOut(id);

        if (result.success()) {
            ctx.json(receiptToJson(result.data()));
        } else {
            ctx.status(getStatusCodeForError(result.errorCode()));
            Map<String, String> error = new HashMap<>();
            error.put("error", result.errorCode());
            error.put("message", result.errorMessage());
            ctx.json(error);
        }
    }

    public void getReceipt(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReservationService.Result<ReservationService.ReservationReceipt> result = reservationService.getReceipt(id);

        if (result.success()) {
            ctx.json(receiptToJson(result.data()));
        } else {
            ctx.status(getStatusCodeForError(result.errorCode()));
            Map<String, String> error = new HashMap<>();
            error.put("error", result.errorCode());
            error.put("message", result.errorMessage());
            ctx.json(error);
        }
    }

    private Map<String, Object> toJson(Reservation reservation) {
        Map<String, Object> obj = new HashMap<>();
        obj.put("id", reservation.id);
        obj.put("roomNumber", reservation.roomNumber);
        obj.put("guestName", reservation.guestName);
        obj.put("guestEmail", reservation.guestEmail);
        obj.put("guestCpf", reservation.guestCpf);
        obj.put("checkIn", reservation.checkIn.format(ISO_DATE));
        obj.put("checkOut", reservation.checkOut.format(ISO_DATE));
        obj.put("guests", reservation.guests);
        obj.put("ratePlan", reservation.ratePlan);
        obj.put("nights", reservation.nights);
        obj.put("subtotal", reservation.subtotalCents / 100.0);
        obj.put("discount", reservation.discountCents / 100.0);
        obj.put("total", reservation.totalCents / 100.0);
        obj.put("status", reservation.status.name());
        obj.put("amountPaid", reservation.amountPaidCents / 100.0);
        obj.put("refund", reservation.refundCents != null ? reservation.refundCents / 100.0 : null);
        return obj;
    }

    private Map<String, Object> receiptToJson(ReservationService.ReservationReceipt receipt) {
        Map<String, Object> obj = new HashMap<>();
        obj.put("title", receipt.title());
        obj.put("reservationId", receipt.reservationId());
        obj.put("guestName", receipt.guestName());
        obj.put("guestCpf", receipt.guestCpf());
        obj.put("roomNumber", receipt.roomNumber());
        obj.put("roomType", receipt.roomType());
        obj.put("checkIn", receipt.checkIn());
        obj.put("checkOut", receipt.checkOut());
        obj.put("nights", receipt.nights());
        obj.put("subtotal", receipt.subtotal() / 100.0);
        obj.put("discount", receipt.discount() / 100.0);
        obj.put("total", receipt.total() / 100.0);
        return obj;
    }

    private int getIntField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        Object value = obj.get(field);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private String getStringField(Map<String, Object> obj, String field) {
        if (!obj.containsKey(field)) {
            throw new IllegalArgumentException("Missing field: " + field);
        }
        return (String) obj.get(field);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    private int getStatusCodeForError(String errorCode) {
        return switch (errorCode) {
            case "ROOM_NOT_FOUND", "RESERVATION_NOT_FOUND" -> 404;
            case "ROOM_UNAVAILABLE", "OPERATION_NOT_ALLOWED" -> 409;
            case "CAPACITY_EXCEEDED" -> 422;
            default -> 400;
        };
    }

    private void sendBadRequest(Context ctx, String code, String message) {
        ctx.status(400);
        Map<String, String> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.json(error);
    }
}
