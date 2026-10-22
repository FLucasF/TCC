package pousada.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import pousada.domain.ApiException;
import pousada.persistence.MessageRepository;
import pousada.service.ReportService;
import pousada.service.ReservationService;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageRepository messages;
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public Routes(ReservationService reservations, ReportService reports, MessageRepository messages) {
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.status()).json(Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(reservations.rooms()));
        app.post("/reservations", ctx ->
                ctx.status(201).json(reservations.create(ReservationRequestParser.parse(body(ctx)))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(ctx.pathParam("id"))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(ctx.pathParam("id"))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> ctx.json(messages.all()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(reports.occupancy(date(ctx))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(reports.revenue(month(ctx))));
        return app;
    }

    private JsonNode body(Context ctx) {
        try {
            return mapper.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
    }

    private static LocalDate date(Context ctx) {
        try {
            return LocalDate.parse(String.valueOf(ctx.queryParam("date")));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Parâmetro date inválido (use AAAA-MM-DD)");
        }
    }

    private static YearMonth month(Context ctx) {
        try {
            return YearMonth.parse(String.valueOf(ctx.queryParam("month")));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Parâmetro month inválido (use AAAA-MM)");
        }
    }
}
