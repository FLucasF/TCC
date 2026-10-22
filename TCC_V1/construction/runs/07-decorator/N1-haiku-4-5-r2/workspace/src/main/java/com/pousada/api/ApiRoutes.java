package com.pousada.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.db.Database;
import com.pousada.models.Message;
import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.services.MessageService;
import com.pousada.services.ReportService;
import com.pousada.services.ReservationService;
import io.javalin.Javalin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiRoutes {
    private final Gson gson = new Gson();
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final MessageService messageService;

    public ApiRoutes(Database db, ReservationService reservationService,
                     ReportService reportService, MessageService messageService) {
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.messageService = messageService;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> {
            List<Map<String, Object>> rooms = new java.util.ArrayList<>();
            for (Room room : Room.FIXED_ROOMS) {
                Map<String, Object> roomMap = new HashMap<>();
                roomMap.put("number", room.getNumber());
                roomMap.put("type", room.getType().name());
                roomMap.put("capacity", room.getCapacity());
                roomMap.put("nightlyRate", room.getNightlyRate().doubleValue());
                rooms.add(roomMap);
            }
            ctx.json(rooms);
        });

        app.post("/reservations", ctx -> {
            try {
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);

                int roomNumber = ((Double) body.get("roomNumber")).intValue();
                String guestName = (String) body.get("guestName");
                String guestEmail = (String) body.get("guestEmail");
                String guestCpf = (String) body.get("guestCpf");
                LocalDate checkIn = LocalDate.parse((String) body.get("checkIn"));
                LocalDate checkOut = LocalDate.parse((String) body.get("checkOut"));
                int guests = ((Double) body.get("guests")).intValue();
                String ratePlan = (String) body.get("ratePlan");

                Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

                ctx.status(201);
                ctx.json(reservationToMap(reservation));
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.get("/reservations/{id}", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.getReservation(id);
                if (reservation == null) {
                    ctx.status(404);
                    ctx.json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                    return;
                }
                ctx.json(reservationToMap(reservation));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Invalid reservation ID"));
            }
        });

        app.post("/reservations/{id}/confirmation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Map<String, Object> body = gson.fromJson(ctx.body(), Map.class);
                BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

                Reservation reservation = reservationService.confirmReservation(id, amountPaid);
                ctx.json(reservationToMap(reservation));
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/{id}/cancellation", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.cancelReservation(id);
                ctx.json(reservationToMap(reservation));
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/{id}/check-in", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Reservation reservation = reservationService.checkIn(id);
                ctx.json(reservationToMap(reservation));
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.post("/reservations/{id}/check-out", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Receipt receipt = reservationService.checkOut(id);
                ctx.json(receiptToMap(receipt));
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.get("/reservations/{id}/document", ctx -> {
            try {
                long id = Long.parseLong(ctx.pathParam("id"));
                Receipt receipt = reservationService.getDocument(id);
                ctx.json(receiptToMap(receipt));
            } catch (Exception e) {
                handleError(ctx, e);
            }
        });

        app.get("/messages", ctx -> {
            List<Map<String, Object>> messages = new java.util.ArrayList<>();
            for (Message msg : messageService.getMessages()) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("channel", msg.getChannel().name());
                msgMap.put("to", msg.getTo());
                msgMap.put("subject", msg.getSubject());
                msgMap.put("body", msg.getBody());
                msgMap.put("status", msg.getStatus().name());
                messages.add(msgMap);
            }
            ctx.json(messages);
        });

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                LocalDate date = LocalDate.parse(dateStr);
                String csv = reportService.getOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Invalid date format"));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                YearMonth.parse(monthStr);
                String csv = reportService.getRevenueReport(monthStr);
                ctx.contentType("text/csv; charset=utf-8");
                ctx.result(csv);
            } catch (Exception e) {
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", "Invalid month format"));
            }
        });
    }

    private Map<String, Object> reservationToMap(Reservation res) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", res.getId());
        map.put("roomNumber", res.getRoomNumber());
        map.put("guestName", res.getGuestName());
        map.put("guestEmail", res.getGuestEmail());
        map.put("guestCpf", res.getGuestCpf());
        map.put("checkIn", res.getCheckIn().toString());
        map.put("checkOut", res.getCheckOut().toString());
        map.put("guests", res.getGuests());
        map.put("ratePlan", res.getRatePlan().name());
        map.put("nights", res.getNights());
        map.put("subtotal", res.getSubtotal().doubleValue());
        map.put("discount", res.getDiscount().doubleValue());
        map.put("total", res.getTotal().doubleValue());
        map.put("status", res.getStatus().name());
        map.put("amountPaid", res.getAmountPaid().doubleValue());
        map.put("refund", res.getRefund() != null ? res.getRefund().doubleValue() : null);
        return map;
    }

    private Map<String, Object> receiptToMap(Receipt receipt) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", receipt.getReservationId());
        map.put("guestName", receipt.getGuestName());
        map.put("guestCpf", receipt.getGuestCpf());
        map.put("roomNumber", receipt.getRoomNumber());
        map.put("roomType", receipt.getRoomType());
        map.put("checkIn", receipt.getCheckIn().toString());
        map.put("checkOut", receipt.getCheckOut().toString());
        map.put("nights", receipt.getNights());
        map.put("subtotal", receipt.getSubtotal().doubleValue());
        map.put("discount", receipt.getDiscount().doubleValue());
        map.put("total", receipt.getTotal().doubleValue());
        return map;
    }

    private Map<String, Object> errorResponse(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        return error;
    }

    private void handleError(io.javalin.http.Context ctx, Exception e) {
        String message = e.getMessage();

        if (message == null || message.isEmpty()) {
            ctx.status(400);
            ctx.json(errorResponse("INVALID_REQUEST", "Invalid request"));
            return;
        }

        switch (message) {
            case "ROOM_NOT_FOUND":
                ctx.status(404);
                ctx.json(errorResponse("ROOM_NOT_FOUND", "Room not found"));
                break;
            case "RESERVATION_NOT_FOUND":
                ctx.status(404);
                ctx.json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
                break;
            case "ROOM_UNAVAILABLE":
                ctx.status(409);
                ctx.json(errorResponse("ROOM_UNAVAILABLE", "Room is unavailable for those dates"));
                break;
            case "CAPACITY_EXCEEDED":
                ctx.status(422);
                ctx.json(errorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
                break;
            case "OPERATION_NOT_ALLOWED":
                ctx.status(409);
                ctx.json(errorResponse("OPERATION_NOT_ALLOWED", "This operation is not allowed in the current state"));
                break;
            default:
                ctx.status(400);
                ctx.json(errorResponse("INVALID_REQUEST", message));
                break;
        }
    }
}
