package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.domain.DomainException;
import pousada.domain.Outbox;
import pousada.domain.RatePlan;
import pousada.domain.Reports;
import pousada.domain.ReservationService;
import pousada.domain.ReservationService.NewReservation;
import pousada.domain.identification.Identification;
import pousada.domain.identification.IdentificationKinds;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rotas HTTP combinadas com o site. */
public final class Api {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CSV = "text/csv; charset=utf-8";

    private Api() {
    }

    public static Javalin create(ReservationService service, Outbox outbox) {
        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> ctx.json(service.rooms().stream().map(Views::room).toList()));

        app.post("/reservations", ctx ->
                ctx.status(201).json(Views.reservation(service.create(parseNewReservation(ctx)))));
        app.get("/reservations/{id}", ctx -> ctx.json(Views.reservation(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> ctx.json(Views.reservation(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> ctx.json(Views.reservation(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> ctx.json(Views.reservation(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> ctx.json(Views.document(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> ctx.json(Views.document(service.document(ctx.pathParam("id")))));

        app.get("/messages", ctx -> ctx.json(outbox.sent().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"), "date");
            ctx.contentType(CSV).result(Reports.occupancy(date, service.all(), service.rooms()));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month = parseMonth(ctx.queryParam("month"));
            ctx.contentType(CSV).result(Reports.revenue(month, service.all()));
        });

        app.exception(DomainException.class,
                (e, ctx) -> ctx.status(e.status()).json(error(e.code(), e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(error("INTERNAL_ERROR", "Erro inesperado"));
        });
        return app;
    }

    private static Map<String, String> error(String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        return body;
    }

    private static NewReservation parseNewReservation(Context ctx) {
        JsonNode body = parseBody(ctx.body());
        Map<String, String> given = new LinkedHashMap<>();
        for (String name : IdentificationKinds.allFieldNames()) {
            String value = optionalText(body, name);
            if (value != null) {
                given.put(name, value);
            }
        }
        Identification identification = IdentificationKinds.parse(given);
        return new NewReservation(
                requiredInt(body, "roomNumber"),
                requiredText(body, "guestName"),
                requiredText(body, "guestEmail"),
                identification,
                parseDate(optionalText(body, "checkIn"), "checkIn"),
                parseDate(optionalText(body, "checkOut"), "checkOut"),
                requiredInt(body, "guests"),
                parseRatePlan(requiredText(body, "ratePlan")));
    }

    private static JsonNode parseBody(String raw) {
        try {
            JsonNode node = MAPPER.readTree(raw);
            if (node != null && node.isObject()) {
                return node;
            }
        } catch (JsonProcessingException ignored) {
            // cai na mensagem abaixo
        }
        throw DomainException.invalidRequest("O corpo deve ser um objeto JSON");
    }

    /** Texto do campo, ou null se ausente ou nulo; um valor que não é texto é erro. */
    private static String optionalText(JsonNode body, String name) {
        JsonNode node = body.get(name);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw DomainException.invalidRequest("O campo " + name + " deve ser texto");
        }
        return node.asText();
    }

    private static String requiredText(JsonNode body, String name) {
        String value = optionalText(body, name);
        if (value == null || value.isBlank()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente: " + name);
        }
        return value.trim();
    }

    private static int requiredInt(JsonNode body, String name) {
        JsonNode node = body.get(name);
        if (node == null || !node.isInt()) {
            throw DomainException.invalidRequest("O campo " + name + " deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static LocalDate parseDate(String value, String name) {
        if (value == null || value.isBlank()) {
            throw DomainException.invalidRequest("Campo obrigatório ausente: " + name);
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Data inválida em " + name + ": use o formato 2027-07-10");
        }
    }

    private static YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            throw DomainException.invalidRequest("Parâmetro obrigatório ausente: month");
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Mês inválido: use o formato 2027-07");
        }
    }

    private static RatePlan parseRatePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw DomainException.invalidRequest("Plano de tarifa inválido: " + value);
        }
    }
}
