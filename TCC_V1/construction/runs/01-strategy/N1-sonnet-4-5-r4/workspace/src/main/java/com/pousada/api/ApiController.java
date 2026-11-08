package com.pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.domain.*;
import com.pousada.repository.*;
import com.pousada.service.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiController {
    private final ObjectMapper objectMapper;
    private final RoomRepository roomRepository;
    private final ReservationService reservationService;
    private final MessageService messageService;
    private final ReportService reportService;

    public ApiController() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();

        this.messageService = new MessageService(messageRepository);
        this.reservationService = new ReservationService(reservationRepository, roomRepository, messageService);
        this.reportService = new ReportService(reservationRepository, roomRepository);
    }

    public void registerRoutes(Javalin app) {
        app.get("/rooms", this::getRooms);
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", this::getReservation);
        app.post("/reservations/{id}/confirmation", this::confirmReservation);
        app.post("/reservations/{id}/cancellation", this::cancelReservation);
        app.post("/reservations/{id}/check-in", this::checkInReservation);
        app.post("/reservations/{id}/check-out", this::checkOutReservation);
        app.get("/reservations/{id}/document", this::getDocument);
        app.get("/messages", this::getMessages);
        app.get("/reports/occupancy", this::getOccupancyReport);
        app.get("/reports/revenue", this::getRevenueReport);

        app.exception(InvalidRequestException.class, (e, ctx) ->
            sendError(ctx, 400, "INVALID_REQUEST", e.getMessage()));
        app.exception(RoomNotFoundException.class, (e, ctx) ->
            sendError(ctx, 404, "ROOM_NOT_FOUND", e.getMessage()));
        app.exception(ReservationNotFoundException.class, (e, ctx) ->
            sendError(ctx, 404, "RESERVATION_NOT_FOUND", e.getMessage()));
        app.exception(RoomUnavailableException.class, (e, ctx) ->
            sendError(ctx, 409, "ROOM_UNAVAILABLE", e.getMessage()));
        app.exception(OperationNotAllowedException.class, (e, ctx) ->
            sendError(ctx, 409, "OPERATION_NOT_ALLOWED", e.getMessage()));
        app.exception(CapacityExceededException.class, (e, ctx) ->
            sendError(ctx, 422, "CAPACITY_EXCEEDED", e.getMessage()));
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        List<Map<String, Object>> response = rooms.stream()
            .map(room -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("number", room.getNumber());
                map.put("type", room.getType().name());
                map.put("capacity", room.getCapacity());
                map.put("nightlyRate", room.getNightlyRate());
                return map;
            })
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private void createReservation(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);

            validateRequiredFields(body, "roomNumber", "guestName", "guestEmail", "guestCpf",
                                  "checkIn", "checkOut", "guests", "ratePlan");

            Reservation reservation = new Reservation();
            reservation.setRoomNumber((String) body.get("roomNumber"));
            reservation.setGuestName((String) body.get("guestName"));
            reservation.setGuestEmail((String) body.get("guestEmail"));
            reservation.setGuestCpf((String) body.get("guestCpf"));

            reservation.setCheckIn(parseDate((String) body.get("checkIn")));
            reservation.setCheckOut(parseDate((String) body.get("checkOut")));
            reservation.setGuests(((Number) body.get("guests")).intValue());
            reservation.setRatePlanType(RatePlanType.valueOf((String) body.get("ratePlan")));
            reservation.setCompanyCode((String) body.get("companyCode"));

            Reservation saved = reservationService.createReservation(reservation);
            ctx.status(201).json(toReservationResponse(saved));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new InvalidRequestException("Dados da requisição inválidos");
        }
    }

    private void getReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.findById(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void confirmReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirm(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void cancelReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancel(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void checkInReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void checkOutReservation(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkOut(id);
        ctx.json(toDocumentResponse(reservation));
    }

    private void getDocument(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.findById(id);

        if (!reservation.isPaid()) {
            throw new OperationNotAllowedException("Comprovante disponível apenas para reservas pagas");
        }

        ctx.json(toDocumentResponse(reservation));
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageService.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(msg -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", msg.getId());
                map.put("channel", msg.getChannel());
                map.put("to", msg.getTo());
                map.put("subject", msg.getSubject());
                map.put("body", msg.getBody());
                return map;
            })
            .collect(Collectors.toList());
        ctx.json(response);
    }

    private void getOccupancyReport(Context ctx) {
        String dateStr = ctx.queryParam("date");
        if (dateStr == null) {
            throw new InvalidRequestException("Parâmetro 'date' é obrigatório");
        }

        try {
            LocalDate date = LocalDate.parse(dateStr);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de data inválido");
        }
    }

    private void getRevenueReport(Context ctx) {
        String monthStr = ctx.queryParam("month");
        if (monthStr == null) {
            throw new InvalidRequestException("Parâmetro 'month' é obrigatório");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthStr);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de mês inválido");
        }
    }

    private Map<String, Object> toReservationResponse(Reservation r) {
        return Map.ofEntries(
            Map.entry("id", r.getId()),
            Map.entry("roomNumber", r.getRoomNumber()),
            Map.entry("guestName", r.getGuestName()),
            Map.entry("guestEmail", r.getGuestEmail()),
            Map.entry("guestCpf", r.getGuestCpf()),
            Map.entry("checkIn", r.getCheckIn().toString()),
            Map.entry("checkOut", r.getCheckOut().toString()),
            Map.entry("guests", r.getGuests()),
            Map.entry("ratePlan", r.getRatePlanType().name()),
            Map.entry("nights", r.getNights()),
            Map.entry("subtotal", r.getSubtotal()),
            Map.entry("discount", r.getDiscount()),
            Map.entry("total", r.getTotal()),
            Map.entry("status", r.getStatus().name()),
            Map.entry("amountPaid", r.getAmountPaid()),
            Map.entry("refund", r.getRefund())
        );
    }

    private Map<String, Object> toDocumentResponse(Reservation r) {
        String roomType = switch (r.getRoomNumber()) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", "Comprovante de reserva");
        map.put("reservationId", r.getId());
        map.put("guestName", r.getGuestName());
        map.put("guestCpf", r.getGuestCpf());
        map.put("roomNumber", r.getRoomNumber());
        map.put("roomType", roomType);
        map.put("checkIn", r.getCheckIn().toString());
        map.put("checkOut", r.getCheckOut().toString());
        map.put("nights", r.getNights());
        map.put("subtotal", r.getSubtotal());
        map.put("discount", r.getDiscount());
        map.put("total", r.getTotal());
        return map;
    }

    private void validateRequiredFields(Map<String, Object> body, String... fields) {
        for (String field : fields) {
            if (!body.containsKey(field) || body.get(field) == null) {
                throw new InvalidRequestException("Campo obrigatório ausente: " + field);
            }
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Formato de data inválido");
        }
    }

    private void sendError(Context ctx, int status, String errorCode, String message) {
        ctx.status(status).json(Map.of(
            "error", errorCode,
            "message", message
        ));
    }
}
