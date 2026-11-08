package com.pousada.api;

import com.google.gson.JsonObject;
import com.pousada.exception.ApiException;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.EmailService;
import com.pousada.service.ReservationService;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void createReservation(Context ctx) {
        try {
            JsonObject body = ctx.bodyAsClass(JsonObject.class);

            int roomNumber = body.get("roomNumber").getAsInt();
            String guestName = getStringOrNull(body, "guestName");
            String guestEmail = getStringOrNull(body, "guestEmail");
            String guestCpf = getStringOrNull(body, "guestCpf");
            LocalDate checkIn = parseDate(getStringOrNull(body, "checkIn"));
            LocalDate checkOut = parseDate(getStringOrNull(body, "checkOut"));
            int guests = body.get("guests").getAsInt();
            String ratePlan = getStringOrNull(body, "ratePlan");

            Reservation reservation = ReservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201);
            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid request body");
        }
    }

    public static void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationRepository.findById(id);

            if (reservation == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        }
    }

    public static void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            JsonObject body = ctx.bodyAsClass(JsonObject.class);
            BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();

            Reservation reservation = ReservationService.confirm(id, amountPaid);
            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid request body");
        }
    }

    public static void checkInReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.checkIn(id);
            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        }
    }

    public static void checkOutReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.checkOut(id);
            ctx.json(getVoucher(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        }
    }

    public static void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationService.cancel(id);
            ctx.json(reservationToJson(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        }
    }

    public static void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = ReservationRepository.findById(id);

            if (reservation == null) {
                throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found");
            }

            ctx.json(getVoucher(reservation));
        } catch (ApiException e) {
            sendErrorResponse(ctx, e);
        }
    }

    private static JsonObject reservationToJson(Reservation r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", r.getId());
        obj.addProperty("roomNumber", r.getRoomNumber());
        obj.addProperty("guestName", r.getGuestName());
        obj.addProperty("guestEmail", r.getGuestEmail());
        obj.addProperty("guestCpf", r.getGuestCpf());
        obj.addProperty("checkIn", r.getCheckIn().format(DATE_FORMATTER));
        obj.addProperty("checkOut", r.getCheckOut().format(DATE_FORMATTER));
        obj.addProperty("guests", r.getGuests());
        obj.addProperty("ratePlan", r.getRatePlan().getName());
        obj.addProperty("nights", r.getNights());
        obj.addProperty("subtotal", r.getSubtotal());
        obj.addProperty("discount", r.getDiscount());
        obj.addProperty("total", r.getTotal());
        obj.addProperty("status", r.getStatus().name());
        obj.addProperty("amountPaid", r.getAmountPaid());
        obj.add("refund", r.getRefund() != null ? new com.google.gson.JsonPrimitive(r.getRefund()) : null);
        return obj;
    }

    private static JsonObject getVoucher(Reservation r) {
        Room room = RoomRepository.findByNumber(r.getRoomNumber());
        String roomType = room.getType().getName();

        JsonObject obj = new JsonObject();
        obj.addProperty("title", "Comprovante de reserva");
        obj.addProperty("reservationId", r.getId());
        obj.addProperty("guestName", r.getGuestName());
        obj.addProperty("guestCpf", r.getGuestCpf());
        obj.addProperty("roomNumber", r.getRoomNumber());
        obj.addProperty("roomType", roomType);
        obj.addProperty("checkIn", r.getCheckIn().format(DATE_FORMATTER));
        obj.addProperty("checkOut", r.getCheckOut().format(DATE_FORMATTER));
        obj.addProperty("nights", r.getNights());
        obj.addProperty("subtotal", r.getSubtotal());
        obj.add("discount", r.getDiscount() != null ? new com.google.gson.JsonPrimitive(r.getDiscount()) : null);
        obj.addProperty("total", r.getTotal());
        return obj;
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    private static void sendErrorResponse(Context ctx, ApiException e) {
        ctx.status(e.getStatusCode());
        JsonObject error = new JsonObject();
        error.addProperty("error", e.getErrorCode());
        error.addProperty("message", e.getMessage());
        ctx.json(error);
    }
}
