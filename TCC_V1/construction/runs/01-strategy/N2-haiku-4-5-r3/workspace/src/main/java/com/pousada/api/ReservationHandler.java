package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.database.ReservationDao;
import com.pousada.model.Reservation;
import com.pousada.model.RatePlanType;
import com.pousada.service.ReservationService;
import com.pousada.service.ApiException;
import com.pousada.util.Dates;
import com.pousada.util.Money;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ReservationHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void create(Context ctx) throws Exception {
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);

            // Validate required fields
            String[] requiredFields = {"roomNumber", "guestName", "guestEmail", "guestCpf",
                    "checkIn", "checkOut", "guests", "ratePlan"};
            for (String field : requiredFields) {
                if (!body.containsKey(field)) {
                    sendError(ctx, "INVALID_REQUEST", "Missing required field: " + field, 400);
                    return;
                }
            }

            int roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            LocalDate checkIn = Dates.parseIso((String) body.get("checkIn"));
            LocalDate checkOut = Dates.parseIso((String) body.get("checkOut"));
            int guests = ((Number) body.get("guests")).intValue();
            RatePlanType ratePlan = RatePlanType.valueOf((String) body.get("ratePlan"));
            String companyCode = (String) body.get("companyCode");

            Reservation reservation = ReservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan, companyCode
            );

            ctx.status(201).json(reservationToDto(reservation));
        } catch (IllegalArgumentException e) {
            sendError(ctx, "INVALID_REQUEST", "Invalid rate plan or date format", 400);
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public static void get(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservation(id);
            ctx.json(reservationToDto(reservation));
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public static void confirm(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservation(id);

            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            if (!body.containsKey("amountPaid")) {
                sendError(ctx, "INVALID_REQUEST", "Missing amountPaid", 400);
                return;
            }

            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());
            Reservation confirmed = ReservationService.confirmReservation(id, amountPaid);
            ctx.json(reservationToDto(confirmed));
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public static void cancel(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation cancelled = ReservationService.cancelReservation(id);
            ctx.json(reservationToDto(cancelled));
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public static void checkIn(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation checkedIn = ReservationService.checkInReservation(id);
            ctx.json(reservationToDto(checkedIn));
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public static void checkOut(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation checkedOut = ReservationService.checkOutReservation(id);
            String receipt = ReservationService.generateReceipt(checkedOut);
            ctx.json(receiptToDto(checkedOut, receipt));
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    public static void document(Context ctx) throws Exception {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Reservation reservation = ReservationService.getReservation(id);

            if (reservation.amountPaid.compareTo(BigDecimal.ZERO) == 0) {
                sendError(ctx, "OPERATION_NOT_ALLOWED", "Only paid reservations have receipts", 409);
                return;
            }

            String receipt = ReservationService.generateReceipt(reservation);
            ctx.json(receiptToDto(reservation, receipt));
        } catch (ApiException e) {
            sendError(ctx, e.code, e.getMessage(), e.statusCode);
        } catch (Exception e) {
            sendError(ctx, "INVALID_REQUEST", e.getMessage(), 400);
        }
    }

    private static Map<String, Object> reservationToDto(Reservation r) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", r.id);
        map.put("roomNumber", r.roomNumber);
        map.put("guestName", r.guestName);
        map.put("guestEmail", r.guestEmail);
        map.put("guestCpf", r.guestCpf);
        map.put("checkIn", Dates.formatCsv(r.checkIn));
        map.put("checkOut", Dates.formatCsv(r.checkOut));
        map.put("guests", r.guests);
        map.put("ratePlan", r.ratePlan.name());
        map.put("nights", r.nights);
        map.put("subtotal", r.subtotal);
        map.put("discount", r.discount);
        map.put("total", r.total);
        map.put("status", r.status.name());
        map.put("amountPaid", r.amountPaid);
        map.put("refund", r.refund);
        return map;
    }

    private static Map<String, Object> receiptToDto(Reservation r, String receipt) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", r.id);
        map.put("guestName", r.guestName);
        map.put("guestCpf", r.guestCpf);
        map.put("roomNumber", r.roomNumber);
        map.put("roomType", "");
        map.put("checkIn", Dates.formatCsv(r.checkIn));
        map.put("checkOut", Dates.formatCsv(r.checkOut));
        map.put("nights", r.nights);
        map.put("subtotal", r.subtotal);
        map.put("discount", r.discount);
        map.put("total", r.total);
        return map;
    }

    private static void sendError(Context ctx, String code, String message, int status) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
