package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Receipt;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.Map;

public class ReservationsApiHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void register(Javalin app, ReservationService reservationService,
                               ReservationRepository reservationRepo) {
        app.post("/reservations", ctx -> {
            try {
                Map<String, Object> body = ctx.bodyAsClass(Map.class);

                Object roomNumberObj = body.get("roomNumber");
                Object guestNameObj = body.get("guestName");
                Object guestEmailObj = body.get("guestEmail");
                Object guestCpfObj = body.get("guestCpf");
                Object checkInObj = body.get("checkIn");
                Object checkOutObj = body.get("checkOut");
                Object guestsObj = body.get("guests");
                Object ratePlanObj = body.get("ratePlan");

                if (roomNumberObj == null || guestNameObj == null || guestEmailObj == null ||
                    guestCpfObj == null || checkInObj == null || checkOutObj == null ||
                    guestsObj == null || ratePlanObj == null) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
                    return;
                }

                int roomNumber = ((Number) roomNumberObj).intValue();
                String guestName = (String) guestNameObj;
                String guestEmail = (String) guestEmailObj;
                String guestCpf = (String) guestCpfObj;
                LocalDate checkIn = LocalDate.parse((String) checkInObj);
                LocalDate checkOut = LocalDate.parse((String) checkOutObj);
                int guests = ((Number) guestsObj).intValue();
                RatePlan ratePlan = RatePlan.valueOf((String) ratePlanObj);

                Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.confirmReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(reservationToJson(reservation));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Receipt receipt = reservationService.checkOut(id);
                ctx.json(receiptToJson(receipt));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Receipt receipt = reservationService.getReceipt(id);
                ctx.json(receiptToJson(receipt));
            } catch (IllegalArgumentException e) {
                handleError(ctx, e.getMessage());
            }
        });
    }

    private static Map<String, Object> reservationToJson(Reservation res) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", res.getId());
        result.put("roomNumber", res.getRoomNumber());
        result.put("guestName", res.getGuestName());
        result.put("guestEmail", res.getGuestEmail());
        result.put("guestCpf", res.getGuestCpf());
        result.put("checkIn", res.getCheckIn().toString());
        result.put("checkOut", res.getCheckOut().toString());
        result.put("guests", res.getGuests());
        result.put("ratePlan", res.getRatePlan().toString());
        result.put("nights", res.getNights());
        result.put("subtotal", res.getSubtotal().doubleValue());
        result.put("discount", res.getDiscount().doubleValue());
        result.put("total", res.getTotal().doubleValue());
        result.put("status", res.getStatus().toString());
        result.put("amountPaid", res.getAmountPaid().doubleValue());
        result.put("refund", res.getRefund());
        return result;
    }

    private static Map<String, Object> receiptToJson(Receipt receipt) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("title", receipt.getTitle());
        result.put("reservationId", receipt.getReservationId());
        result.put("guestName", receipt.getGuestName());
        result.put("guestCpf", receipt.getGuestCpf());
        result.put("roomNumber", receipt.getRoomNumber());
        result.put("roomType", receipt.getRoomType().toString());
        result.put("checkIn", receipt.getCheckIn().toString());
        result.put("checkOut", receipt.getCheckOut().toString());
        result.put("nights", receipt.getNights());
        result.put("subtotal", receipt.getSubtotal().doubleValue());
        result.put("discount", receipt.getDiscount().doubleValue());
        result.put("total", receipt.getTotal().doubleValue());
        return result;
    }

    private static void handleError(io.javalin.http.Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND":
                ctx.status(404);
                ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404);
                ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409);
                ctx.json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room unavailable for this period"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed for current reservation status"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422);
                ctx.json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Too many guests for this room"));
                break;
            default:
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", error));
        }
    }
}
