package com.pousadamarealta.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiHandler {
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final MessageRepository messageRepository;
    private final ReportService reportService;

    public ApiHandler(RoomRepository roomRepository,
                      ReservationService reservationService,
                      ReservationRepository reservationRepository,
                      MessageRepository messageRepository,
                      ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.messageRepository = messageRepository;
        this.reportService = reportService;
    }

    public void getRooms(Context ctx) {
        List<Map<String, Object>> rooms = roomRepository.findAll().stream()
                .map(room -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("number", room.getNumber());
                    map.put("type", room.getType().toString());
                    map.put("capacity", room.getCapacity());
                    map.put("nightlyRate", room.getNightlyRate());
                    return map;
                })
                .collect(Collectors.toList());

        ctx.json(rooms);
    }

    public void createReservation(Context ctx) {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            String roomNumber = getRequiredString(json, "roomNumber");
            String guestName = getRequiredString(json, "guestName");
            String guestEmail = getRequiredString(json, "guestEmail");
            String guestCpf = getRequiredString(json, "guestCpf");
            LocalDate checkIn = getRequiredDate(json, "checkIn");
            LocalDate checkOut = getRequiredDate(json, "checkOut");
            int guests = getRequiredInt(json, "guests");
            RatePlan ratePlan = getRequiredEnum(json, "ratePlan", RatePlan.class);

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan);

            ctx.status(HttpStatus.CREATED);
            ctx.json(toReservationMap(reservation));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationMap(reservation));
    }

    public void confirmReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationMap(reservation));
    }

    public void cancelReservation(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationMap(reservation));
    }

    public void checkIn(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationMap(reservation));
    }

    public void checkOut(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReceiptDocument receipt = reservationService.checkOut(id);
        ctx.json(toReceiptMap(receipt));
    }

    public void getDocument(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        ReceiptDocument receipt = reservationService.getDocument(id);
        ctx.json(toReceiptMap(receipt));
    }

    public void getMessages(Context ctx) {
        List<Map<String, Object>> messages = messageRepository.findAll().stream()
                .map(message -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", message.getId());
                    map.put("channel", message.getChannel());
                    map.put("to", message.getTo());
                    map.put("subject", message.getSubject());
                    map.put("body", message.getBody());
                    return map;
                })
                .collect(Collectors.toList());

        ctx.json(messages);
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateParam);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        }
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRevenueReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getTourismTaxReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateTourismTaxReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void getRatePlansReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
            return;
        }

        try {
            YearMonth month = YearMonth.parse(monthParam);
            String csv = reportService.generateRatePlansReport(month);
            ctx.contentType("text/csv; charset=utf-8");
            ctx.result(csv);
        } catch (DateTimeParseException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        }
    }

    public void handleException(Context ctx, Exception e) {
        if (e instanceof InvalidRequestException) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        } else if (e instanceof RoomNotFoundException) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("ROOM_NOT_FOUND", e.getMessage()));
        } else if (e instanceof ReservationNotFoundException) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("RESERVATION_NOT_FOUND", e.getMessage()));
        } else if (e instanceof RoomUnavailableException) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("ROOM_UNAVAILABLE", e.getMessage()));
        } else if (e instanceof OperationNotAllowedException) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED", e.getMessage()));
        } else if (e instanceof CapacityExceededException) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            ctx.json(new ErrorResponse("CAPACITY_EXCEEDED", e.getMessage()));
        } else {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    private Map<String, Object> toReservationMap(Reservation r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("roomNumber", r.getRoomNumber());
        map.put("guestName", r.getGuestName());
        map.put("guestEmail", r.getGuestEmail());
        map.put("guestCpf", r.getGuestCpf());
        map.put("checkIn", r.getCheckIn());
        map.put("checkOut", r.getCheckOut());
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

    private Map<String, Object> toReceiptMap(ReceiptDocument r) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", r.getTitle());
        map.put("reservationId", r.getReservationId());
        map.put("guestName", r.getGuestName());
        map.put("guestCpf", r.getGuestCpf());
        map.put("roomNumber", r.getRoomNumber());
        map.put("roomType", r.getRoomType());
        map.put("checkIn", r.getCheckIn());
        map.put("checkOut", r.getCheckOut());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        return map;
    }

    private String getRequiredString(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return json.get(field).asText();
    }

    private int getRequiredInt(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return json.get(field).asInt();
    }

    private LocalDate getRequiredDate(JsonNode json, String field) {
        String value = getRequiredString(json, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for field: " + field);
        }
    }

    private <E extends Enum<E>> E getRequiredEnum(JsonNode json, String field, Class<E> enumClass) {
        String value = getRequiredString(json, field);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for field: " + field);
        }
    }
}
