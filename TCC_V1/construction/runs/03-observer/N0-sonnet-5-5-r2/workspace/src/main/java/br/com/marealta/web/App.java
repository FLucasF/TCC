package br.com.marealta.web;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.NewReservation;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class App {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public App(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(reservations.listRooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(reservations.create(parseNewReservation(ctx))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.listMessages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(reports.occupancy(dateParam(ctx, "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(reports.revenue(monthParam(ctx, "month"))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno."));
        });
        return app;
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        ctx.status(code.httpStatus()).json(Map.of("error", code.name(), "message", message));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente.");
        }
    }

    private static LocalDate dateParam(Context ctx, String name) {
        String value = ctx.queryParam(name);
        try {
            if (value != null) {
                return LocalDate.parse(value);
            }
        } catch (DateTimeParseException ignored) {
            // cai no erro abaixo
        }
        throw invalid("Parâmetro '" + name + "' ausente ou fora do formato AAAA-MM-DD.");
    }

    private static YearMonth monthParam(Context ctx, String name) {
        String value = ctx.queryParam(name);
        try {
            if (value != null) {
                return YearMonth.parse(value);
            }
        } catch (DateTimeParseException ignored) {
            // cai no erro abaixo
        }
        throw invalid("Parâmetro '" + name + "' ausente ou fora do formato AAAA-MM.");
    }

    private NewReservation parseNewReservation(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.body());
        } catch (Exception e) {
            throw invalid("O corpo da requisição não é um JSON válido.");
        }
        if (body == null || !body.isObject()) {
            throw invalid("O corpo da requisição deve ser um objeto JSON.");
        }
        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo 'ratePlan' inválido.");
        }
        return new NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo '" + field + "' ausente ou inválido.");
        }
        return node.asText().strip();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo '" + field + "' ausente ou inválido.");
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode body, String field) {
        String value = text(body, field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Campo '" + field + "' fora do formato AAAA-MM-DD.");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.INVALID_REQUEST, message);
    }
}
