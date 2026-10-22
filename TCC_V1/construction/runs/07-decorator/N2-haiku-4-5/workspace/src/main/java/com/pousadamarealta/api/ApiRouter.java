package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousadamarealta.db.MessageRepository;
import com.pousadamarealta.db.ReservationRepository;
import com.pousadamarealta.db.RoomRepository;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.PricingService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiRouter {
    private final Javalin app;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final PricingService pricingService;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ApiRouter(Javalin app, RoomRepository roomRepository, ReservationRepository reservationRepository,
                    MessageRepository messageRepository, PricingService pricingService,
                    ReservationService reservationService, MessageService messageService,
                    ReportService reportService) {
        this.app = app;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.pricingService = pricingService;
        this.reservationService = reservationService;
        this.messageService = messageService;
        this.reportService = reportService;
        this.objectMapper = new ObjectMapper();
    }

    public void setupRoutes() {
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
        try {
            List<com.pousadamarealta.model.Room> rooms = roomRepository.findAll();
            List<Map<String, Object>> roomList = rooms.stream().map(room -> {
                Map<String, Object> map = new HashMap<>();
                map.put("number", room.number());
                map.put("type", room.type());
                map.put("capacity", room.capacity());
                map.put("nightlyRate", room.nightlyRate());
                return map;
            }).toList();
            ctx.json(roomList);
        } catch (Exception e) {
            errorResponse(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            Integer roomNumber = ((Number) body.get("roomNumber")).intValue();
            String guestName = (String) body.get("guestName");
            String guestEmail = (String) body.get("guestEmail");
            String guestCpf = (String) body.get("guestCpf");
            String checkInStr = (String) body.get("checkIn");
            String checkOutStr = (String) body.get("checkOut");
            Integer guests = ((Number) body.get("guests")).intValue();
            String ratePlan = (String) body.get("ratePlan");

            if (roomNumber == null || guestName == null || guestEmail == null || guestCpf == null ||
                checkInStr == null || checkOutStr == null || guests == null || ratePlan == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan
            );

            ctx.status(201).json(reservationToMap(reservation));
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void getReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));
            ctx.json(reservationToMap(reservation));
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void confirmReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            BigDecimal amountPaid = new BigDecimal(body.get("amountPaid").toString());

            Reservation reservation = reservationService.confirmReservation(id, amountPaid);
            ctx.json(reservationToMap(reservation));
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void cancelReservation(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.cancelReservation(id);
            ctx.json(reservationToMap(reservation));
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void checkIn(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkIn(id);
            ctx.json(reservationToMap(reservation));
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void checkOut(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationService.checkOut(id);
            com.pousadamarealta.model.Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            String receipt = reservationService.generateReceipt(reservation, room);

            Map<String, Object> result = new HashMap<>();
            result.put("title", "Comprovante de reserva");
            result.put("reservationId", reservation.getId());
            result.put("guestName", reservation.getGuestName());
            result.put("guestCpf", formatCpf(reservation.getGuestCpf()));
            result.put("roomNumber", reservation.getRoomNumber());
            result.put("roomType", room.type());
            result.put("checkIn", reservation.getCheckIn().toString());
            result.put("checkOut", reservation.getCheckOut().toString());
            result.put("nights", reservation.getNights());
            result.put("subtotal", reservation.getSubtotal());
            result.put("discount", reservation.getDiscount());
            result.put("total", reservation.getTotal());

            ctx.json(result);
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void getDocument(Context ctx) {
        try {
            Long id = Long.parseLong(ctx.pathParam("id"));
            Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RESERVATION_NOT_FOUND"));

            if (!("CONFIRMED".equals(reservation.getStatus()) || "CHECKED_IN".equals(reservation.getStatus()) ||
                  "CHECKED_OUT".equals(reservation.getStatus()))) {
                errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", "Document only available for paid reservations");
                return;
            }

            com.pousadamarealta.model.Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            String receipt = reservationService.generateReceipt(reservation, room);

            Map<String, Object> result = new HashMap<>();
            result.put("title", "Comprovante de reserva");
            result.put("reservationId", reservation.getId());
            result.put("guestName", reservation.getGuestName());
            result.put("guestCpf", formatCpf(reservation.getGuestCpf()));
            result.put("roomNumber", reservation.getRoomNumber());
            result.put("roomType", room.type());
            result.put("checkIn", reservation.getCheckIn().toString());
            result.put("checkOut", reservation.getCheckOut().toString());
            result.put("nights", reservation.getNights());
            result.put("subtotal", reservation.getSubtotal());
            result.put("discount", reservation.getDiscount());
            result.put("total", reservation.getTotal());

            ctx.json(result);
        } catch (IllegalArgumentException e) {
            handleReservationError(ctx, e.getMessage());
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void getMessages(Context ctx) {
        try {
            List<Message> messages = messageRepository.findAll();
            List<Map<String, Object>> messageList = messages.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("channel", msg.getChannel());
                map.put("to", msg.getTo());
                map.put("subject", msg.getSubject());
                map.put("body", msg.getBody());
                map.put("status", msg.getStatus());
                return map;
            }).toList();
            ctx.json(messageList);
        } catch (Exception e) {
            errorResponse(ctx, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private void getOccupancyReport(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing date parameter");
                return;
            }
            LocalDate date = LocalDate.parse(dateStr);
            String report = reportService.generateOccupancyReport(date);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void getRevenueReport(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) {
                errorResponse(ctx, 400, "INVALID_REQUEST", "Missing month parameter");
                return;
            }
            String[] parts = monthStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            String report = reportService.generateRevenueReport(year, month);
            ctx.header("Content-Type", "text/csv; charset=utf-8");
            ctx.result(report);
        } catch (Exception e) {
            errorResponse(ctx, 400, "INVALID_REQUEST", e.getMessage());
        }
    }

    private void handleReservationError(Context ctx, String error) {
        switch (error) {
            case "ROOM_NOT_FOUND" -> errorResponse(ctx, 404, "ROOM_NOT_FOUND", "Room not found");
            case "RESERVATION_NOT_FOUND" -> errorResponse(ctx, 404, "RESERVATION_NOT_FOUND", "Reservation not found");
            case "ROOM_UNAVAILABLE" -> errorResponse(ctx, 409, "ROOM_UNAVAILABLE", "Room is not available for these dates");
            case "CAPACITY_EXCEEDED" -> errorResponse(ctx, 422, "CAPACITY_EXCEEDED", "Guest count exceeds room capacity");
            case "OPERATION_NOT_ALLOWED" -> errorResponse(ctx, 409, "OPERATION_NOT_ALLOWED", "Operation not allowed for this reservation status");
            default -> errorResponse(ctx, 400, "INVALID_REQUEST", error);
        }
    }

    private void errorResponse(Context ctx, int status, String error, String message) {
        ctx.status(status);
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", error);
        errorMap.put("message", message);
        ctx.json(errorMap);
    }

    private Map<String, Object> reservationToMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("guests", r.getGuests());
        map.put("ratePlan", r.getRatePlan());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        map.put("status", r.getStatus());
        map.put("amountPaid", r.getAmountPaid());
        map.put("refund", r.getRefund());
        return map;
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
