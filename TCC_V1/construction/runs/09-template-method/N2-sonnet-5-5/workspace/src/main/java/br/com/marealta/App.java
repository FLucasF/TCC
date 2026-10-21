package br.com.marealta;

import br.com.marealta.reports.OccupancyReport;
import br.com.marealta.reports.RatePlanReport;
import br.com.marealta.reports.Report;
import br.com.marealta.reports.RevenueReport;
import br.com.marealta.reports.TourismTaxReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class App {
    /** Taxa de turismo da prefeitura, por hóspede por noite. */
    private static final String TOURISM_TAX = "4.50";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Rooms rooms = new Rooms();
    private final Outbox outbox = new Outbox();
    private final ReservationService reservations = new ReservationService(rooms, outbox);
    private final List<Report<?, ?>> reports = List.of(
            new OccupancyReport(reservations, rooms),
            new RevenueReport(reservations),
            new TourismTaxReport(reservations, Money.of(TOURISM_TAX)),
            new RatePlanReport(reservations));

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));

        app.get("/rooms", ctx -> json(ctx, 200, rooms.all()));
        app.post("/reservations", ctx -> json(ctx, 201, create(ctx)));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservations.get(ctx.pathParam("id"))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservations.confirm(ctx.pathParam("id"))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservations.cancel(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservations.checkIn(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.all()));

        for (Report<?, ?> report : reports) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = report.render(ctx.queryParam(report.paramName()));
                ctx.status(200).contentType("text/csv; charset=utf-8").result(csv);
            });
        }
        return app;
    }

    private Reservation create(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw ApiException.invalid("Corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan = ratePlan(body);
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede");
        }
        return reservations.create(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalid("Campo " + field + " ausente ou inválido");
        }
        return node.intValue();
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid("Campo " + field + " ausente ou inválido");
        }
        return node.asText();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Campo " + field + " deve estar no formato 2027-07-10");
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        try {
            return RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("Campo ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private void json(Context ctx, int status, Object value) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        json(ctx, status, Map.of("error", code, "message", message));
    }
}
