package br.com.marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** A interface HTTP combinada com o site. */
public class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final Rooms rooms;
    private final ReservationService service;
    private final ReportService reports;

    public Routes(Rooms rooms, ReservationService service, ReportService reports) {
        this.rooms = rooms;
        this.service = service;
        this.reports = reports;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> reply(ctx, 200, rooms.all()));
        app.post("/reservations", ctx -> reply(ctx, 201, service.create(parseNewReservation(ctx.body()))));
        app.get("/reservations/{id}", ctx -> reply(ctx, 200, service.get(ctx.pathParam("id"))));
        app.post("/reservations/{id}/confirmation", ctx -> reply(ctx, 200, service.confirm(ctx.pathParam("id"))));
        app.post("/reservations/{id}/cancellation", ctx -> reply(ctx, 200, service.cancel(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-in", ctx -> reply(ctx, 200, service.checkIn(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-out", ctx -> reply(ctx, 200, service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> reply(ctx, 200, service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> reply(ctx, 200, service.sentMessages()));
        app.get("/reports/occupancy", ctx ->
                ctx.status(200).contentType(CSV).result(reports.occupancy(date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx ->
                ctx.status(200).contentType(CSV).result(reports.revenue(month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) ->
                reply(ctx, e.status(), Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            reply(ctx, 500, Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
    }

    private void reply(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(json.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private NewReservation parseNewReservation(String body) {
        JsonNode node;
        try {
            node = json.readTree(body);
        } catch (JsonProcessingException e) {
            throw DomainException.invalidRequest("O corpo não é um JSON válido");
        }
        if (node == null || !node.isObject()) {
            throw DomainException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        int guests = integer(node, "guests");
        return new NewReservation(
                integer(node, "roomNumber"),
                text(node, "guestName"),
                text(node, "guestEmail"),
                text(node, "guestCpf"),
                date(text(node, "checkIn"), "checkIn"),
                date(text(node, "checkOut"), "checkOut"),
                guests,
                ratePlan(text(node, "ratePlan")));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return value.asText();
    }

    private static int integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isInt()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return value.asInt();
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw DomainException.invalidRequest("Plano de tarifa inválido: " + value);
        }
    }

    private static LocalDate date(String value, String field) {
        if (value == null) throw DomainException.invalidRequest("Falta o parâmetro " + field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Data inválida em " + field + ": use AAAA-MM-DD");
        }
    }

    private static YearMonth month(String value) {
        if (value == null) throw DomainException.invalidRequest("Falta o parâmetro month");
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Mês inválido: use AAAA-MM");
        }
    }
}
