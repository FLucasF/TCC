package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.database.RoomDAO;
import com.pousadamarealta.domain.Money;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.service.GuestNotificationService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ReservationsHandler {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    public static void createReservation(Context ctx) throws Exception {
        JsonNode body = ctx.bodyAsClass(JsonNode.class);

        if (!body.hasNonNull("roomNumber") || !body.hasNonNull("guestName") ||
            !body.hasNonNull("guestEmail") || !body.hasNonNull("guestCpf") ||
            !body.hasNonNull("checkIn") || !body.hasNonNull("checkOut") ||
            !body.hasNonNull("guests") || !body.hasNonNull("ratePlan")) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
            return;
        }

        int roomNumber = body.get("roomNumber").asInt();
        String guestName = body.get("guestName").asText();
        String guestEmail = body.get("guestEmail").asText();
        String guestCpf = body.get("guestCpf").asText();
        LocalDate checkIn = LocalDate.parse(body.get("checkIn").asText(), DATE_FORMATTER);
        LocalDate checkOut = LocalDate.parse(body.get("checkOut").asText(), DATE_FORMATTER);
        int guests = body.get("guests").asInt();
        String ratePlan = body.get("ratePlan").asText();

        Room room = RoomDAO.getInstance().getRoomByNumber(roomNumber);
        if (room == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
            return;
        }

        if (guests < 1 || guests > room.getCapacity()) {
            ctx.status(422);
            ctx.json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Guest count exceeds room capacity"));
            return;
        }

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Check-out must be after check-in"));
            return;
        }

        if (!ReservationService.getInstance().isRoomAvailable(roomNumber, checkIn, checkOut)) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room not available for these dates"));
            return;
        }

        Reservation reservation = ReservationService.getInstance().createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

        ctx.status(HttpStatus.CREATED);
        ctx.json(mapReservation(reservation));
    }

    public static void getReservation(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = ReservationService.getInstance().getReservation(id);

        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        ctx.json(mapReservation(reservation));
    }

    public static void confirmReservation(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        JsonNode body = ctx.bodyAsClass(JsonNode.class);

        if (!body.hasNonNull("amountPaid")) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Missing amountPaid"));
            return;
        }

        Reservation reservation = ReservationService.getInstance().getReservation(id);
        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        if (reservation.getStatus().getCode().equals("PENDING")) {
            try {
                Money amountPaid = Money.of(body.get("amountPaid").asDouble());
                reservation = ReservationService.getInstance().confirmReservation(id, amountPaid);
                ctx.json(mapReservation(reservation));
            } catch (RuntimeException e) {
                ctx.status(HttpStatus.CONFLICT);
                ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
            }
        } else {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Cannot confirm non-pending reservation"));
        }
    }

    public static void cancelReservation(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = ReservationService.getInstance().getReservation(id);

        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        try {
            reservation = ReservationService.getInstance().cancelReservation(id);
            ctx.json(mapReservation(reservation));
        } catch (RuntimeException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
        }
    }

    public static void checkIn(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = ReservationService.getInstance().getReservation(id);

        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        try {
            reservation = ReservationService.getInstance().checkIn(id);
            ctx.json(mapReservation(reservation));
        } catch (RuntimeException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
        }
    }

    public static void checkOut(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = ReservationService.getInstance().getReservation(id);

        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        try {
            reservation = ReservationService.getInstance().checkOut(id);
            ctx.json(mapCheckOutResponse(reservation));
        } catch (RuntimeException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", e.getMessage()));
        }
    }

    public static void getDocument(Context ctx) throws Exception {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = ReservationService.getInstance().getReservation(id);

        if (reservation == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        if (reservation.getAmountPaid().toDouble() == 0) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Document only available for paid reservations"));
            return;
        }

        ctx.json(mapDocument(reservation));
    }

    private static Map<String, Object> mapReservation(Reservation res) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", res.getId());
        map.put("roomNumber", res.getRoomNumber());
        map.put("guestName", res.getGuestName());
        map.put("guestEmail", res.getGuestEmail());
        map.put("guestCpf", res.getGuestCpf());
        map.put("checkIn", res.getCheckIn().format(DATE_FORMATTER));
        map.put("checkOut", res.getCheckOut().format(DATE_FORMATTER));
        map.put("guests", res.getGuests());
        map.put("ratePlan", res.getRatePlan().getCode());
        map.put("nights", res.getNights());
        map.put("subtotal", Math.round(res.getSubtotal().toDouble() * 100) / 100.0);
        map.put("discount", Math.round(res.getDiscount().toDouble() * 100) / 100.0);
        map.put("total", Math.round(res.getTotal().toDouble() * 100) / 100.0);
        map.put("status", res.getStatus().getCode());
        map.put("amountPaid", Math.round(res.getAmountPaid().toDouble() * 100) / 100.0);
        map.put("refund", res.getRefund() != null ? Math.round(res.getRefund().toDouble() * 100) / 100.0 : null);
        return map;
    }

    private static Map<String, Object> mapCheckOutResponse(Reservation res) {
        return mapReservation(res);
    }

    private static Map<String, Object> mapDocument(Reservation res) {
        String roomType;
        if (res.getRoomNumber() < 200) {
            roomType = "STANDARD";
        } else if (res.getRoomNumber() < 300) {
            roomType = "DELUXE";
        } else {
            roomType = "SUITE";
        }

        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", res.getId());
        map.put("guestName", res.getGuestName());
        map.put("guestCpf", res.getGuestCpf());
        map.put("roomNumber", res.getRoomNumber());
        map.put("roomType", roomType);
        map.put("checkIn", res.getCheckIn().format(DATE_FORMATTER));
        map.put("checkOut", res.getCheckOut().format(DATE_FORMATTER));
        map.put("nights", res.getNights());
        map.put("subtotal", Math.round(res.getSubtotal().toDouble() * 100) / 100.0);
        map.put("discount", Math.round(res.getDiscount().toDouble() * 100) / 100.0);
        map.put("total", Math.round(res.getTotal().toDouble() * 100) / 100.0);
        return map;
    }
}
