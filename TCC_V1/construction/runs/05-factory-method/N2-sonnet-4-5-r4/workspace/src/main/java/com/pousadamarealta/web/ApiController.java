package com.pousadamarealta.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.pousadamarealta.document.Document;
import com.pousadamarealta.domain.*;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public final class ApiController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final ReservationService reservationService;
    private final ReportService reportService;

    public ApiController(RoomRepository roomRepository, MessageRepository messageRepository,
                        ReservationService reservationService, ReportService reportService) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.reservationService = reservationService;
        this.reportService = reportService;
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

        app.exception(ReservationService.RoomNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(Map.of("error", "ROOM_NOT_FOUND", "message", "Room not found"));
        });
        app.exception(ReservationService.ReservationNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
        });
        app.exception(ReservationService.RoomUnavailableException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of("error", "ROOM_UNAVAILABLE", "message", "Room is not available for the selected dates"));
        });
        app.exception(ReservationService.CapacityExceededException.class, (e, ctx) -> {
            ctx.status(422).json(Map.of("error", "CAPACITY_EXCEEDED", "message", "Number of guests exceeds room capacity"));
        });
        app.exception(ReservationService.OperationNotAllowedException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of("error", "OPERATION_NOT_ALLOWED", "message", "Operation not allowed in current reservation state"));
        });
        app.exception(ReservationService.InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        });
        app.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        });
    }

    private void getRooms(Context ctx) {
        List<Room> rooms = roomRepository.findAll();
        ctx.json(rooms.stream()
            .map(r -> Map.of(
                "number", r.number(),
                "type", r.type(),
                "capacity", r.capacity(),
                "nightlyRate", r.nightlyRate()
            ))
            .toList());
    }

    private void createReservation(Context ctx) {
        try {
            JsonNode json = ctx.bodyAsClass(JsonNode.class);

            int roomNumber = getRequiredInt(json, "roomNumber");
            String guestName = getRequiredString(json, "guestName");
            String guestEmail = getRequiredString(json, "guestEmail");
            LocalDate checkIn = parseDate(getRequiredString(json, "checkIn"));
            LocalDate checkOut = parseDate(getRequiredString(json, "checkOut"));
            int guests = getRequiredInt(json, "guests");
            RatePlan ratePlan = RatePlan.valueOf(getRequiredString(json, "ratePlan"));

            GuestIdentification guestId = parseGuestIdentification(json);

            Reservation reservation = reservationService.createReservation(
                roomNumber, guestName, guestEmail, guestId, checkIn, checkOut, guests, ratePlan);

            ctx.status(201).json(toReservationResponse(reservation));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private void getReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.getReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void confirmReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.confirmReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void cancelReservation(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.cancelReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void checkIn(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Reservation reservation = reservationService.checkInReservation(id);
        ctx.json(toReservationResponse(reservation));
    }

    private void checkOut(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Document document = reservationService.checkOutReservation(id);
        ctx.json(toDocumentResponse(document));
    }

    private void getDocument(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Document document = reservationService.getDocument(id);
        ctx.json(toDocumentResponse(document));
    }

    private void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        ctx.json(messages.stream()
            .map(m -> Map.of(
                "id", m.id(),
                "channel", m.channel(),
                "to", m.to(),
                "subject", m.subject(),
                "body", m.body()
            ))
            .toList());
    }

    private void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null) {
            throw new InvalidRequestException("Missing date parameter");
        }

        try {
            LocalDate date = LocalDate.parse(dateParam, DATE_FORMATTER);
            String csv = reportService.generateOccupancyReport(date);
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format");
        }
    }

    private void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null) {
            throw new InvalidRequestException("Missing month parameter");
        }

        try {
            YearMonth yearMonth = YearMonth.parse(monthParam, MONTH_FORMATTER);
            String csv = reportService.generateRevenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid month format");
        }
    }

    private GuestIdentification parseGuestIdentification(JsonNode json) {
        boolean hasCpf = json.has("guestCpf") && !json.get("guestCpf").isNull();
        boolean hasPassport = json.has("guestPassport") && !json.get("guestPassport").isNull();
        boolean hasCnpj = json.has("companyCnpj") && !json.get("companyCnpj").isNull();

        int count = (hasCpf ? 1 : 0) + (hasPassport ? 1 : 0) + (hasCnpj ? 1 : 0);
        if (count != 1) {
            throw new InvalidRequestException("Exactly one of guestCpf, guestPassport, or companyCnpj must be provided");
        }

        if (hasCpf) {
            return new GuestIdentification.IndividualGuest(json.get("guestCpf").asText());
        } else if (hasPassport) {
            String passport = json.get("guestPassport").asText();
            String country = getRequiredString(json, "guestCountry");
            return new GuestIdentification.ForeignGuest(passport, country);
        } else {
            String cnpj = json.get("companyCnpj").asText();
            String companyName = getRequiredString(json, "companyName");
            return new GuestIdentification.CorporateGuest(cnpj, companyName);
        }
    }

    private Map<String, Object> toReservationResponse(Reservation r) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", r.id());
        map.put("roomNumber", r.roomNumber());
        map.put("guestName", r.guestName());
        map.put("guestEmail", r.guestEmail());

        if (r.guestIdentification() instanceof GuestIdentification.IndividualGuest individual) {
            map.put("guestCpf", individual.cpf());
            map.put("guestPassport", null);
            map.put("guestCountry", null);
            map.put("companyCnpj", null);
            map.put("companyName", null);
        } else if (r.guestIdentification() instanceof GuestIdentification.ForeignGuest foreign) {
            map.put("guestCpf", null);
            map.put("guestPassport", foreign.passport());
            map.put("guestCountry", foreign.country());
            map.put("companyCnpj", null);
            map.put("companyName", null);
        } else if (r.guestIdentification() instanceof GuestIdentification.CorporateGuest corporate) {
            map.put("guestCpf", null);
            map.put("guestPassport", null);
            map.put("guestCountry", null);
            map.put("companyCnpj", corporate.cnpj());
            map.put("companyName", corporate.companyName());
        }

        map.put("checkIn", r.checkIn());
        map.put("checkOut", r.checkOut());
        map.put("guests", r.guests());
        map.put("ratePlan", r.ratePlan());
        map.put("nights", r.nights());
        map.put("subtotal", r.subtotal());
        map.put("discount", r.discount());
        map.put("total", r.total());
        map.put("status", r.status());
        map.put("amountPaid", r.amountPaid());
        map.put("refund", r.refund());

        return map;
    }

    private Map<String, Object> toDocumentResponse(Document d) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("title", d.title());
        map.put("reservationId", d.reservationId());
        map.put("guestName", d.guestName());
        map.put("guestCpf", d.guestCpf());
        map.put("guestPassport", d.guestPassport());
        map.put("guestCountry", d.guestCountry());
        map.put("companyCnpj", d.companyCnpj());
        map.put("companyName", d.companyName());
        map.put("roomNumber", d.roomNumber());
        map.put("roomType", d.roomType());
        map.put("checkIn", d.checkIn());
        map.put("checkOut", d.checkOut());
        map.put("nights", d.nights());
        map.put("subtotal", d.subtotal());
        map.put("discount", d.discount());
        map.put("total", d.total());
        map.put("text", d.text());
        return map;
    }

    private String getRequiredString(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new InvalidRequestException("Missing required field: " + field);
        }
        return json.get(field).asText();
    }

    private int getRequiredInt(JsonNode json, String field) {
        if (!json.has(field) || json.get(field).isNull()) {
            throw new InvalidRequestException("Missing required field: " + field);
        }
        return json.get(field).asInt();
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("Invalid date format: " + date);
        }
    }

    static class InvalidRequestException extends RuntimeException {
        InvalidRequestException(String message) {
            super(message);
        }
    }
}
