package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.domain.ApiException;
import pousada.service.CreateReservation;
import pousada.service.ReportService;
import pousada.service.ReservationService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Api {
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ReservationService reservations;
    private final ReportService reports;

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin build() {
        Javalin app = Javalin.create();
        app.get("/rooms", ctx -> respond(ctx, 200, reservations.rooms()));
        app.post("/reservations", ctx -> respond(ctx, 201, reservations.create(parseCreate(ctx.body()))));
        app.get("/reservations/{id}", ctx -> respond(ctx, 200, reservations.get(ctx.pathParam("id"))));
        app.post("/reservations/{id}/confirmation", ctx -> respond(ctx, 200, reservations.confirm(ctx.pathParam("id"))));
        app.post("/reservations/{id}/cancellation", ctx -> respond(ctx, 200, reservations.cancel(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-in", ctx -> respond(ctx, 200, reservations.checkIn(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-out", ctx -> respond(ctx, 200, reservations.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> respond(ctx, 200, reservations.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> respond(ctx, 200, reservations.messages()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(
                parseDate(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));
        app.exception(ApiException.class, (e, ctx) ->
                respond(ctx, e.status(), Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            respond(ctx, 500, Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private void respond(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(json.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private CreateReservation parseCreate(String body) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
        if (root == null || !root.isObject()) {
            throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");
        }
        JsonNode company = root.get("companyCode");
        return new CreateReservation(
                integer(root, "roomNumber"), text(root, "guestName"), text(root, "guestEmail"),
                text(root, "guestCpf"), parseDate(text(root, "checkIn"), "checkIn"),
                parseDate(text(root, "checkOut"), "checkOut"), integer(root, "guests"),
                text(root, "ratePlan"), company != null && company.isTextual() ? company.asText() : null);
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            if (value == null) throw new DateTimeParseException("ausente", "", 0);
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + ", use AAAA-MM-DD");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            if (value == null) throw new DateTimeParseException("ausente", "", 0);
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Mês inválido, use AAAA-MM");
        }
    }
}
