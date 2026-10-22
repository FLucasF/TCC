package com.pousada;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.model.RatePlan;
import com.pousada.model.Reservation;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.DocumentService;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteConfig {
    private static final Gson gson = new Gson();

    public static void setupRoutes(Javalin app) {
        app.get("/rooms", ctx -> {
            var rooms = RoomRepository.findAll();
            ctx.json(rooms.stream().map(r -> Map.of(
                "number", r.getNumber(),
                "type", r.getType(),
                "capacity", r.getCapacity(),
                "nightlyRate", r.getNightlyRate()
            )).toList());
        });

        app.post("/reservations", ctx -> {
            try {
                JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

                if (!body.has("roomNumber") || !body.has("guestName") || !body.has("guestEmail") ||
                    !body.has("guestCpf") || !body.has("checkIn") || !body.has("checkOut") ||
                    !body.has("guests") || !body.has("ratePlan")) {
                    throw new BadRequestResponse("Missing required fields");
                }

                int roomNumber = body.get("roomNumber").getAsInt();
                String guestName = body.get("guestName").getAsString();
                String guestEmail = body.get("guestEmail").getAsString();
                String guestCpf = body.get("guestCpf").getAsString();
                String checkInStr = body.get("checkIn").getAsString();
                String checkOutStr = body.get("checkOut").getAsString();
                int guests = body.get("guests").getAsInt();
                String ratePlanStr = body.get("ratePlan").getAsString();

                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
                LocalDate checkIn = LocalDate.parse(checkInStr, formatter);
                LocalDate checkOut = LocalDate.parse(checkOutStr, formatter);

                if (checkOut.compareTo(checkIn) <= 0) {
                    throw new BadRequestResponse("Checkout must be after checkin");
                }

                if (guests < 1) {
                    throw new BadRequestResponse("Guests must be at least 1");
                }

                RatePlan ratePlan = RatePlan.valueOf(ratePlanStr);

                Reservation reservation = ReservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

                ctx.status(201);
                ctx.json(buildReservationResponse(reservation));
            } catch (BadRequestResponse e) {
                throw e;
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var reservation = ReservationRepository.findById(id);
                if (reservation.isEmpty()) {
                    ctx.status(404);
                    ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                    return;
                }
                ctx.json(buildReservationResponse(reservation.get()));
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

                if (!body.has("amountPaid")) {
                    throw new BadRequestResponse("Missing amountPaid");
                }

                BigDecimal amountPaid = body.get("amountPaid").getAsBigDecimal();
                Reservation reservation = ReservationService.confirmReservation(id, amountPaid);
                ctx.json(buildReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.cancelReservation(id);
                ctx.json(buildReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkIn(id);
                ctx.json(buildReservationResponse(reservation));
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                Reservation reservation = ReservationService.checkOut(id);
                String receipt = DocumentService.generateReceipt(reservation);

                var room = RoomRepository.findByNumber(reservation.getRoomNumber()).get();
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("title", "Comprovante de reserva");
                response.put("reservationId", reservation.getId());
                response.put("guestName", reservation.getGuestName());
                response.put("guestCpf", reservation.getGuestCpf());
                response.put("roomNumber", reservation.getRoomNumber());
                response.put("roomType", room.getType());
                response.put("checkIn", reservation.getCheckIn().toString());
                response.put("checkOut", reservation.getCheckOut().toString());
                response.put("nights", reservation.getNights());
                response.put("subtotal", reservation.getSubtotal());
                response.put("discount", reservation.getDiscount());
                response.put("total", reservation.getTotal());

                ctx.json(response);
            } catch (IllegalArgumentException e) {
                handleReservationError(ctx, e);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                int id = Integer.parseInt(ctx.pathParam("id"));
                var reservationOpt = ReservationRepository.findById(id);
                if (reservationOpt.isEmpty()) {
                    ctx.status(404);
                    ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
                    return;
                }
                Reservation reservation = reservationOpt.get();

                var room = RoomRepository.findByNumber(reservation.getRoomNumber()).get();
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("title", "Comprovante de reserva");
                response.put("reservationId", reservation.getId());
                response.put("guestName", reservation.getGuestName());
                response.put("guestCpf", reservation.getGuestCpf());
                response.put("roomNumber", reservation.getRoomNumber());
                response.put("roomType", room.getType());
                response.put("checkIn", reservation.getCheckIn().toString());
                response.put("checkOut", reservation.getCheckOut().toString());
                response.put("nights", reservation.getNights());
                response.put("subtotal", reservation.getSubtotal());
                response.put("discount", reservation.getDiscount());
                response.put("total", reservation.getTotal());

                ctx.json(response);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/messages", ctx -> {
            var messages = MessageRepository.findAll();
            ctx.json(messages.stream().map(m -> Map.of(
                "id", m.getId(),
                "channel", m.getChannel(),
                "to", m.getTo(),
                "subject", m.getSubject(),
                "body", m.getBody()
            )).toList());
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null) {
                    throw new BadRequestResponse("Missing date parameter");
                }
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                String report = ReportService.generateOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    throw new BadRequestResponse("Missing month parameter");
                }
                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = ReportService.generateRevenueReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reports/tourism-tax", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    throw new BadRequestResponse("Missing month parameter");
                }
                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = ReportService.generateTourismTaxReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });

        app.get("/reports/rate-plans", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null) {
                    throw new BadRequestResponse("Missing month parameter");
                }
                YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
                String report = ReportService.generateRatePlansReport(month);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(report);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
            }
        });
    }

    private static void handleReservationError(io.javalin.http.Context ctx, IllegalArgumentException e) {
        String message = e.getMessage();
        if ("ROOM_NOT_FOUND".equals(message)) {
            ctx.status(404);
            ctx.json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
        } else if ("RESERVATION_NOT_FOUND".equals(message)) {
            ctx.status(404);
            ctx.json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
        } else if ("ROOM_UNAVAILABLE".equals(message)) {
            ctx.status(409);
            ctx.json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room is not available for these dates"));
        } else if ("CAPACITY_EXCEEDED".equals(message)) {
            ctx.status(422);
            ctx.json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Too many guests for this room"));
        } else if ("OPERATION_NOT_ALLOWED".equals(message)) {
            ctx.status(409);
            ctx.json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "This operation is not allowed for the current reservation status"));
        } else {
            ctx.status(400);
            ctx.json(Map.of("error", "INVALID_REQUEST", "message", message));
        }
    }

    private static Map<String, Object> buildReservationResponse(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("guests", r.getGuests());
        map.put("ratePlan", r.getRatePlan().toString());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        map.put("status", r.getStatus().toString());
        map.put("amountPaid", r.getAmountPaid());
        map.put("refund", r.getRefund());
        return map;
    }
}
