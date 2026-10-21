package com.pousadamarealta.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Receipt;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import com.pousadamarealta.util.DateUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;

public class ApiRoutes {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;
    private final Gson gson;

    public ApiRoutes(RoomService roomService, ReservationService reservationService,
                     MessageService messageService, ReportService reportService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    public void setupRoutes(Javalin app) {
        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkIn);
        app.post("/reservations/{id}/check-out", this::checkOut);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);
    }

    private void getRooms(Context ctx) {
        var rooms = roomService.getAllRooms().stream()
                .map(r -> Map.of(
                        "number", r.getNumber(),
                        "type", r.getType(),
                        "capacity", r.getCapacity(),
                        "nightlyRate", r.getNightlyRate().setScale(2, java.math.RoundingMode.HALF_EVEN).doubleValue()
                ))
                .toList();
        ctx.json(rooms);
    }

    private void createReservation(Context ctx) {
        try {
            var body = gson.fromJson(ctx.body(), Map.class);

            if (body == null || !body.containsKey("roomNumber") || !body.containsKey("guestName") ||
                !body.containsKey("guestEmail") || !body.containsKey("guestCpf") ||
                !body.containsKey("checkIn") || !body.containsKey("checkOut") ||
                !body.containsKey("guests") || !body.containsKey("ratePlan")) {
                error(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            int roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            LocalDate checkIn = LocalDate.parse((String) body.get("checkIn"));
            LocalDate checkOut = LocalDate.parse((String) body.get("checkOut"));
            int guests = ((Number) body.get("guests")).intValue();
            RatePlan ratePlan = RatePlan.valueOf((String) body.get("ratePlan"));

            if (guests < 1) {
                error(ctx, 400, "INVALID_REQUEST", "At least one guest is required");
                return;
            }

            var roomOpt = roomService.getRoomByNumber(roomNumber);
            if (roomOpt.isEmpty()) {
                error(ctx, 404, "ROOM_NOT_FOUND", "Room does not exist");
                return;
            }

            var r = roomOpt.get();
            if (guests > r.getCapacity()) {
                error(ctx, 422, "CAPACITY_EXCEEDED", "Too many guests for this room");
                return;
            }

            if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
                error(ctx, 400, "INVALID_REQUEST", "Check-out must be after check-in");
                return;
            }

            try {
                Reservation reservation = reservationService.createReservation(
                        roomNumber, guestName, guestEmail, guestCpf,
                        checkIn, checkOut, guests, ratePlan
                );

                ctx.status(201);
                ctx.json(serializeReservation(reservation));
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Room unavailable")) {
                    error(ctx, 409, "ROOM_UNAVAILABLE", "Room is unavailable for the requested dates");
                } else {
                    throw e;
                }
            }
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request format");
        } catch (IllegalArgumentException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request format");
        } catch (JsonSyntaxException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid JSON");
        }
    }

    private void getReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation does not exist");
                return;
            }

            ctx.json(serializeReservation(reservation.get()));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            var body = gson.fromJson(ctx.body(), Map.class);

            if (body == null || !body.containsKey("amountPaid")) {
                error(ctx, 400, "INVALID_REQUEST", "Missing amountPaid field");
                return;
            }

            BigDecimal amountPaid = new BigDecimal(String.valueOf(body.get("amountPaid")));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation does not exist");
                return;
            }

            if (reservation.get().getStatus().toString().equals("PENDING")) {
                var confirmed = reservationService.confirmReservation(id, amountPaid);
                ctx.json(serializeReservation(confirmed));
            } else {
                error(ctx, 409, "OPERATION_NOT_ALLOWED", "Cannot confirm a non-pending reservation");
            }
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid request format");
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation does not exist");
                return;
            }

            var cancelled = reservationService.cancelReservation(id);
            ctx.json(serializeReservation(cancelled));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (IllegalArgumentException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    private void checkIn(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation does not exist");
                return;
            }

            var checkedIn = reservationService.checkIn(id);
            ctx.json(serializeReservation(checkedIn));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (IllegalArgumentException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    private void checkOut(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation does not exist");
                return;
            }

            Receipt receipt = reservationService.checkOut(id);
            ctx.json(serializeReceipt(receipt));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (IllegalArgumentException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    private void getDocument(Context ctx) {
        try {
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Reservation> reservation = reservationService.getReservation(id);

            if (reservation.isEmpty()) {
                error(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation does not exist");
                return;
            }

            Receipt receipt = reservationService.getReceipt(id);
            ctx.json(serializeReceipt(receipt));
        } catch (NumberFormatException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid reservation ID");
        } catch (IllegalArgumentException e) {
            error(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage());
        }
    }

    private void getMessages(Context ctx) {
        var messages = messageService.getAllMessages().stream()
                .map(m -> Map.of(
                        "id", m.getId(),
                        "channel", m.getChannel().toString(),
                        "to", m.getTo(),
                        "subject", m.getSubject(),
                        "body", m.getBody(),
                        "status", m.getStatus().toString()
                ))
                .toList();
        ctx.json(messages);
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null || dateStr.isEmpty()) {
                error(ctx, 400, "INVALID_REQUEST", "Missing date parameter");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr);
            String report = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (IllegalArgumentException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null || monthStr.isEmpty()) {
                error(ctx, 400, "INVALID_REQUEST", "Missing month parameter");
                return;
            }

            YearMonth month = YearMonth.parse(monthStr);
            String report = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(report);
        } catch (IllegalArgumentException e) {
            error(ctx, 400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        ctx.status(status);
        ctx.json(Map.of(
                "error", code,
                "message", message
        ));
    }

    private Map<String, Object> serializeReservation(Reservation r) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
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
        map.put("subtotal", roundToTwoDecimals(r.getSubtotal()));
        map.put("discount", roundToTwoDecimals(r.getDiscount()));
        map.put("total", roundToTwoDecimals(r.getTotal()));
        map.put("status", r.getStatus().toString());
        map.put("amountPaid", roundToTwoDecimals(r.getAmountPaid()));
        map.put("refund", r.getRefund() != null ? roundToTwoDecimals(r.getRefund()) : null);
        return map;
    }

    private Map<String, Object> serializeReceipt(Receipt r) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", r.getReservationId());
        map.put("guestName", r.getGuestName());
        map.put("guestCpf", r.getGuestCpf());
        map.put("roomNumber", r.getRoomNumber());
        map.put("roomType", r.getRoomType());
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("nights", r.getNights());
        map.put("subtotal", roundToTwoDecimals(r.getSubtotal()));
        map.put("discount", roundToTwoDecimals(r.getDiscount()));
        map.put("total", roundToTwoDecimals(r.getTotal()));
        return map;
    }

    private double roundToTwoDecimals(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_EVEN).doubleValue();
    }
}
