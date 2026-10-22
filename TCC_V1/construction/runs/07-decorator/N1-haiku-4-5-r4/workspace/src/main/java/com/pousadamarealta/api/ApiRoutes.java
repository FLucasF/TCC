package com.pousadamarealta.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.dao.RoomDAO;
import com.pousadamarealta.models.*;
import com.pousadamarealta.service.*;

public class ApiRoutes {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void setupRoutes(Javalin app) {
        app.get("/rooms", ApiRoutes::getRooms);
        app.post("/reservations", ApiRoutes::createReservation);
        app.get("/reservations/{id}", ApiRoutes::getReservation);
        app.post("/reservations/{id}/confirmation", ApiRoutes::confirmReservation);
        app.post("/reservations/{id}/cancellation", ApiRoutes::cancelReservation);
        app.post("/reservations/{id}/check-in", ApiRoutes::checkIn);
        app.post("/reservations/{id}/check-out", ApiRoutes::checkOut);
        app.get("/reservations/{id}/document", ApiRoutes::getDocument);
        app.get("/messages", ApiRoutes::getMessages);
        app.get("/reports/occupancy", ApiRoutes::getOccupancyReport);
        app.get("/reports/revenue", ApiRoutes::getRevenueReport);
    }

    private static void getRooms(Context ctx) throws SQLException {
        List<Room> rooms = RoomDAO.getAllRooms();
        List<Map<String, Object>> response = rooms.stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            map.put("number", room.number());
            map.put("type", room.type().name());
            map.put("capacity", room.capacity());
            map.put("nightlyRate", room.nightlyRate().doubleValue());
            return map;
        }).toList();
        ctx.json(response);
    }

    private static void createReservation(Context ctx) throws SQLException {
        try {
            JsonNode body = mapper.readTree(ctx.body());

            if (body.get("roomNumber") == null || body.get("guestName") == null ||
                body.get("guestEmail") == null || body.get("guestCpf") == null ||
                body.get("checkIn") == null || body.get("checkOut") == null ||
                body.get("guests") == null || body.get("ratePlan") == null) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request"));
                return;
            }

            int roomNumber = body.get("roomNumber").asInt();
            String guestName = body.get("guestName").asText();
            String guestEmail = body.get("guestEmail").asText();
            String guestCpf = body.get("guestCpf").asText();
            LocalDate checkIn = LocalDate.parse(body.get("checkIn").asText());
            LocalDate checkOut = LocalDate.parse(body.get("checkOut").asText());
            int guests = body.get("guests").asInt();
            RatePlan ratePlan = RatePlan.valueOf(body.get("ratePlan").asText());

            if (guests < 1 || checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request"));
                return;
            }

            Room room = RoomDAO.getRoomByNumber(roomNumber);
            if (room == null) {
                ctx.status(404);
                ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
                return;
            }

            if (guests > room.capacity()) {
                ctx.status(422);
                ctx.json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Capacity exceeded"));
                return;
            }

            Reservation reservation = ReservationService.createReservation(roomNumber, guestName, guestEmail,
                    guestCpf, checkIn, checkOut, guests, ratePlan);

            if (reservation == null) {
                ctx.status(409);
                ctx.json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room not available"));
                return;
            }

            ctx.status(201);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid request"));
        }
    }

    private static void getReservation(Context ctx) throws SQLException {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = ReservationService.getReservation(id);
        if (reservation == null) {
            ctx.status(404);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }
        ctx.json(reservationToJson(reservation));
    }

    private static void confirmReservation(Context ctx) throws SQLException {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = ReservationService.confirmReservation(id);
        if (reservation == null) {
            ctx.status(409);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed"));
            return;
        }
        ctx.json(reservationToJson(reservation));
    }

    private static void cancelReservation(Context ctx) throws SQLException {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = ReservationService.cancelReservation(id);
        if (reservation == null) {
            ctx.status(409);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed"));
            return;
        }
        ctx.json(reservationToJson(reservation));
    }

    private static void checkIn(Context ctx) throws SQLException {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = ReservationService.checkIn(id);
        if (reservation == null) {
            ctx.status(409);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed"));
            return;
        }
        ctx.json(reservationToJson(reservation));
    }

    private static void checkOut(Context ctx) throws SQLException {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = ReservationService.checkOut(id);
        if (reservation == null) {
            ctx.status(409);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed"));
            return;
        }

        Receipt receipt = ReceiptFormatter.toReceipt(reservation.id(), reservation.guestName(),
                reservation.guestCpf(), reservation.roomNumber(),
                RoomDAO.getRoomByNumber(reservation.roomNumber()).type(),
                reservation.checkIn(), reservation.checkOut(),
                reservation.nights(), reservation.subtotal(),
                reservation.discount(), reservation.total());

        ctx.json(receiptToJson(receipt));
    }

    private static void getDocument(Context ctx) throws SQLException {
        int id = ctx.pathParamAsClass("id", Integer.class).get();
        Reservation reservation = ReservationService.getDocument(id);
        if (reservation == null) {
            ctx.status(404);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
            return;
        }

        if (reservation.amountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            ctx.status(404);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Document not available"));
            return;
        }

        Receipt receipt = ReceiptFormatter.toReceipt(reservation.id(), reservation.guestName(),
                reservation.guestCpf(), reservation.roomNumber(),
                RoomDAO.getRoomByNumber(reservation.roomNumber()).type(),
                reservation.checkIn(), reservation.checkOut(),
                reservation.nights(), reservation.subtotal(),
                reservation.discount(), reservation.total());

        ctx.json(receiptToJson(receipt));
    }

    private static void getMessages(Context ctx) throws SQLException {
        List<Message> messages = MessageService.getAllMessages();
        List<Map<String, Object>> response = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.id());
            map.put("channel", msg.channel().name());
            map.put("to", msg.to());
            map.put("subject", msg.subject());
            map.put("body", msg.body());
            map.put("status", msg.status().name());
            return map;
        }).toList();
        ctx.json(response);
    }

    private static void getOccupancyReport(Context ctx) throws SQLException {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null || dateStr.isEmpty()) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Date parameter required"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            String report = ReportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
        }
    }

    private static void getRevenueReport(Context ctx) throws SQLException {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null || monthStr.isEmpty()) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Month parameter required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthStr);
            String report = ReportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
        }
    }

    private static Map<String, Object> reservationToJson(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.id());
        map.put("roomNumber", r.roomNumber());
        map.put("guestName", r.guestName());
        map.put("guestEmail", r.guestEmail());
        map.put("guestCpf", r.guestCpf());
        map.put("checkIn", r.checkIn().toString());
        map.put("checkOut", r.checkOut().toString());
        map.put("guests", r.guests());
        map.put("ratePlan", r.ratePlan().name());
        map.put("nights", r.nights());
        map.put("subtotal", r.subtotal().doubleValue());
        map.put("discount", r.discount().doubleValue());
        map.put("total", r.total().doubleValue());
        map.put("status", r.status().name());
        map.put("amountPaid", r.amountPaid().doubleValue());
        map.put("refund", r.refund() != null ? r.refund().doubleValue() : null);
        return map;
    }

    private static Map<String, Object> receiptToJson(Receipt r) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", r.title());
        map.put("reservationId", r.reservationId());
        map.put("guestName", r.guestName());
        map.put("guestCpf", r.guestCpf());
        map.put("roomNumber", r.roomNumber());
        map.put("roomType", r.roomType().name());
        map.put("checkIn", r.checkIn().toString());
        map.put("checkOut", r.checkOut().toString());
        map.put("nights", r.nights());
        map.put("subtotal", r.subtotal().doubleValue());
        map.put("discount", r.discount().doubleValue());
        map.put("total", r.total().doubleValue());
        return map;
    }
}
