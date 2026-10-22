package com.pousadamarealta.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousadamarealta.dto.ReceiptDto;
import com.pousadamarealta.exception.*;
import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiController {
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final MessageRepository messageRepository;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public ApiController(
            RoomRepository roomRepository,
            ReservationService reservationService,
            MessageRepository messageRepository,
            ReportService reportService) {
        this.roomRepository = roomRepository;
        this.reservationService = reservationService;
        this.messageRepository = messageRepository;
        this.reportService = reportService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void registerRoutes(Javalin app) {
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
        app.get("/reports/tourism-tax", this::getTourismTaxReport);
        app.get("/reports/rate-plans", this::getRatePlansReport);

        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", e.getMessage()
            ));
        });

        app.exception(RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(Map.of(
                    "error", "ROOM_NOT_FOUND",
                    "message", e.getMessage()
            ));
        });

        app.exception(ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(Map.of(
                    "error", "RESERVATION_NOT_FOUND",
                    "message", e.getMessage()
            ));
        });

        app.exception(RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of(
                    "error", "ROOM_UNAVAILABLE",
                    "message", e.getMessage()
            ));
        });

        app.exception(OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of(
                    "error", "OPERATION_NOT_ALLOWED",
                    "message", e.getMessage()
            ));
        });

        app.exception(CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(Map.of(
                    "error", "CAPACITY_EXCEEDED",
                    "message", e.getMessage()
            ));
        });
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
                .<Map<String, Object>>map(room -> Map.of(
                        "number", room.getNumber(),
                        "type", room.getType().name(),
                        "capacity", room.getCapacity(),
                        "nightlyRate", room.getNightlyRate()
                ))
                .collect(Collectors.toList());
        ctx.json(response);
    }

    private void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            String roomNumber = getRequiredString(body, "roomNumber");
            String guestName = getRequiredString(body, "guestName");
            String guestEmail = getRequiredString(body, "guestEmail");
            String guestCpf = getRequiredString(body, "guestCpf");
            LocalDate checkIn = parseDate(getRequiredString(body, "checkIn"));
            LocalDate checkOut = parseDate(getRequiredString(body, "checkOut"));
            int guests = getRequiredInt(body, "guests");
            RatePlan ratePlan = RatePlan.valueOf(getRequiredString(body, "ratePlan"));

            Reservation reservation = reservationService.createReservation(
                    roomNumber, guestName, guestEmail, guestCpf,
                    checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toReservationDto(reservation));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new InvalidRequestException("Invalid request format");
        }
    }

    private void getReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    private void confirmReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    private void cancelReservation(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationDto(reservation));
    }

    private void checkIn(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationDto(reservation));
    }

    private void checkOut(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        ReceiptDto receipt = reservationService.checkOut(id);
        ctx.json(toReceiptMap(receipt));
    }

    private void getDocument(Context ctx) {
        Long id = parseId(ctx.pathParam("id"));
        ReceiptDto receipt = reservationService.getDocument(id);
        ctx.json(toReceiptMap(receipt));
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
                .<Map<String, Object>>map(msg -> Map.of(
                        "id", msg.getId(),
                        "channel", msg.getChannel(),
                        "to", msg.getTo(),
                        "subject", msg.getSubject(),
                        "body", msg.getBody()
                ))
                .collect(Collectors.toList());
        ctx.json(response);
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new InvalidRequestException("Missing date parameter");
        }
        LocalDate date = parseDate(dateStr);
        String csv = reportService.generateOccupancyReport(date);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }
        var parsed = parseMonth(monthStr);
        String csv = reportService.generateRevenueReport(parsed[0], parsed[1]);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getTourismTaxReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }
        var parsed = parseMonth(monthStr);
        String csv = reportService.generateTourismTaxReport(parsed[0], parsed[1]);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private void getRatePlansReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Missing month parameter");
        }
        var parsed = parseMonth(monthStr);
        String csv = reportService.generateRatePlansReport(parsed[0], parsed[1]);
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private Map<String, Object> toReservationDto(Reservation r) {
        return Map.ofEntries(
                Map.entry("id", r.getId()),
                Map.entry("roomNumber", r.getRoomNumber()),
                Map.entry("guestName", r.getGuestName()),
                Map.entry("guestEmail", r.getGuestEmail()),
                Map.entry("guestCpf", r.getGuestCpf()),
                Map.entry("checkIn", r.getCheckIn().toString()),
                Map.entry("checkOut", r.getCheckOut().toString()),
                Map.entry("guests", r.getGuests()),
                Map.entry("ratePlan", r.getRatePlan().name()),
                Map.entry("nights", r.getNights()),
                Map.entry("subtotal", r.getSubtotal()),
                Map.entry("discount", r.getDiscount()),
                Map.entry("total", r.getTotal()),
                Map.entry("status", r.getStatus().name()),
                Map.entry("amountPaid", r.getAmountPaid()),
                Map.entry("refund", r.getRefund())
        );
    }

    private Map<String, Object> toReceiptMap(ReceiptDto receipt) {
        return Map.ofEntries(
                Map.entry("title", receipt.title()),
                Map.entry("reservationId", receipt.reservationId()),
                Map.entry("guestName", receipt.guestName()),
                Map.entry("guestCpf", receipt.guestCpf()),
                Map.entry("roomNumber", receipt.roomNumber()),
                Map.entry("roomType", receipt.roomType()),
                Map.entry("checkIn", receipt.checkIn().toString()),
                Map.entry("checkOut", receipt.checkOut().toString()),
                Map.entry("nights", receipt.nights()),
                Map.entry("subtotal", receipt.subtotal()),
                Map.entry("discount", receipt.discount()),
                Map.entry("total", receipt.total())
        );
    }

    private String getRequiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new InvalidRequestException("Missing field: " + key);
        }
        return value.toString();
    }

    private int getRequiredInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new InvalidRequestException("Missing field: " + key);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new InvalidRequestException("Invalid integer: " + key);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + dateStr);
        }
    }

    private int[] parseMonth(String monthStr) {
        try {
            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                throw new InvalidRequestException("Invalid month format: " + monthStr);
            }
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) {
                throw new InvalidRequestException("Invalid month: " + month);
            }
            return new int[]{year, month};
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid month format: " + monthStr);
        }
    }

    private Long parseId(String idStr) {
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid ID format");
        }
    }
}
