package br.com.marealta.web;

import br.com.marealta.Reports;
import br.com.marealta.ReservationService;
import br.com.marealta.ReservationService.NewReservation;
import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.persistence.StoredMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final StoredMessages messages;
    private final Reports reports;
    private final ObjectMapper mapper;

    public Routes(ReservationService service, StoredMessages messages, Reports reports, ObjectMapper mapper) {
        this.service = service;
        this.messages = messages;
        this.reports = reports;
        this.mapper = mapper;
    }

    public void register(Javalin app) {
        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Unexpected error");
        });

        app.get("/rooms", ctx -> ctx.json(service.rooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(parseNewReservation(ctx))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(messages.list()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(
                reports.occupancy(parseDate(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(
                reports.revenue(parseMonth(ctx.queryParam("month")))));
    }

    private void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found: " + raw);
        }
    }

    private NewReservation parseNewReservation(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.bodyAsBytes());
        } catch (IOException e) {
            throw invalid("The body is not valid JSON");
        }
        if (body == null || !body.isObject()) {
            throw invalid("The body must be a JSON object");
        }
        LocalDate checkIn = parseDate(text(body, "checkIn"), "checkIn");
        LocalDate checkOut = parseDate(text(body, "checkOut"), "checkOut");
        int guests = integer(body, "guests");
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("checkOut must be after checkIn");
        }
        if (guests < 1) {
            throw invalid("guests must be at least 1");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan is not valid");
        }
        return new NewReservation(integer(body, "roomNumber"), text(body, "guestName"), text(body, "guestEmail"),
                text(body, "guestCpf"), checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid(field + " is required");
        }
        return node.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid(field + " must be an integer");
        }
        return node.asInt();
    }

    private static LocalDate parseDate(String raw, String field) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid(field + " must be a date like 2027-07-10");
        }
    }

    private static YearMonth parseMonth(String raw) {
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("month must be like 2027-07");
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.INVALID_REQUEST, message);
    }
}
