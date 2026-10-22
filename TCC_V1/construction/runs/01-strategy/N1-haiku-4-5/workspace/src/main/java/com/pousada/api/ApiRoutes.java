package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.database.Database;
import com.pousada.database.MessageRepository;
import com.pousada.database.ReservationRepository;
import com.pousada.database.RoomRepository;
import com.pousada.exception.ApiException;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import com.pousada.model.Room;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import com.pousada.util.Formatter;
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
import java.util.stream.Collectors;

public class ApiRoutes {
    private final Database database;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiRoutes(Database database) {
        this.database = database;
        this.roomRepository = new RoomRepository(database);
        this.reservationRepository = new ReservationRepository(database);
        this.messageRepository = new MessageRepository(database);
        this.reservationService = new ReservationService(roomRepository, reservationRepository, messageRepository);
        this.reportService = new ReportService(roomRepository, reservationRepository);
    }

    public void register(Javalin app) {
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

    private void getRooms(Context ctx) throws SQLException {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            map.put("number", room.getNumber());
            map.put("type", room.getType().getDisplayName());
            map.put("capacity", room.getCapacity());
            map.put("nightlyRate", room.getNightlyRate());
            return map;
        }).collect(Collectors.toList());
        ctx.json(response);
    }

    private void createReservation(Context ctx) {
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
            Object companyCodeObj = body.get("companyCode");

            if (roomNumberObj == null || guestNameObj == null || guestEmailObj == null ||
                guestCpfObj == null || checkInObj == null || checkOutObj == null ||
                guestsObj == null || ratePlanObj == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            int roomNumber = ((Number) roomNumberObj).intValue();
            String guestName = guestNameObj.toString();
            String guestEmail = guestEmailObj.toString();
            String guestCpf = guestCpfObj.toString();
            LocalDate checkIn = LocalDate.parse(checkInObj.toString(), DateTimeFormatter.ISO_DATE);
            LocalDate checkOut = LocalDate.parse(checkOutObj.toString(), DateTimeFormatter.ISO_DATE);
            int guests = ((Number) guestsObj).intValue();
            RatePlan ratePlan = RatePlan.valueOf(ratePlanObj.toString().toUpperCase());
            String companyCode = companyCodeObj != null ? companyCodeObj.toString() : null;

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan, companyCode);

            ctx.status(201);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void getReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            Reservation reservation = reservationService.confirmReservation(
                id, new BigDecimal(body.get("amountPaid").toString()));
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void checkIn(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservationToJson(reservation));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void checkOut(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationService.checkOut(id);
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(formatReceipt(reservation, room));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void getDocument(Context ctx) {
        try {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

            if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
                reservation.getStatus() != ReservationStatus.CHECKED_IN &&
                reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", "Document not available for this reservation");
                return;
            }

            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).orElseThrow();
            ctx.json(formatReceipt(reservation, room));
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void getMessages(Context ctx) {
        try {
            var messages = messageRepository.findAll();
            var response = messages.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("channel", msg.getChannel());
                map.put("to", msg.getTo());
                map.put("subject", msg.getSubject());
                map.put("body", msg.getBody());
                return map;
            }).collect(Collectors.toList());
            ctx.json(response);
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing date parameter");
                return;
            }

            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            try (var conn = database.getConnection()) {
                String report = reportService.generateOccupancyReport(date, conn);
                ctx.header("Content-Type", "text/csv;charset=utf-8");
                ctx.result(report);
            }
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing month parameter");
                return;
            }

            YearMonth month = YearMonth.parse(monthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
            try (var conn = database.getConnection()) {
                String report = reportService.generateRevenueReport(month, conn);
                ctx.header("Content-Type", "text/csv;charset=utf-8");
                ctx.result(report);
            }
        } catch (Exception e) {
            handleException(ctx, e);
        }
    }

    private Map<String, Object> reservationToJson(Reservation reservation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reservation.getId());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("guestName", reservation.getGuestName());
        map.put("guestEmail", reservation.getGuestEmail());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("checkIn", Formatter.formatDateIso(reservation.getCheckIn()));
        map.put("checkOut", Formatter.formatDateIso(reservation.getCheckOut()));
        map.put("guests", reservation.getGuests());
        map.put("ratePlan", reservation.getRatePlan());
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        map.put("status", reservation.getStatus());
        map.put("amountPaid", reservation.getAmountPaid());
        map.put("refund", reservation.getRefund());
        return map;
    }

    private Map<String, Object> formatReceipt(Reservation reservation, Room room) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", reservation.getId());
        map.put("guestName", reservation.getGuestName());
        map.put("guestCpf", reservation.getGuestCpf());
        map.put("roomNumber", reservation.getRoomNumber());
        map.put("roomType", room.getType().getDisplayName());
        map.put("checkIn", Formatter.formatDateIso(reservation.getCheckIn()));
        map.put("checkOut", Formatter.formatDateIso(reservation.getCheckOut()));
        map.put("nights", reservation.getNights());
        map.put("subtotal", reservation.getSubtotal());
        map.put("discount", reservation.getDiscount());
        map.put("total", reservation.getTotal());
        return map;
    }

    private void handleException(Context ctx, Exception e) {
        if (e instanceof ApiException apiEx) {
            errorResponse(ctx, apiEx.getStatusCode(), apiEx.getErrorCode(), apiEx.getMessage());
        } else {
            errorResponse(ctx, 500, "INTERNAL_ERROR", "An error occurred");
        }
    }

    private void errorResponse(Context ctx, int status, String code, String message) {
        ctx.status(status);
        Map<String, Object> error = new HashMap<>();
        error.put("error", code);
        error.put("message", message);
        ctx.json(error);
    }
}
