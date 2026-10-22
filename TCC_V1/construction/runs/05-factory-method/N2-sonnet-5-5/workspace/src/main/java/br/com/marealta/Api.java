package br.com.marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongFunction;

public class Api {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RoomCatalog rooms;
    private final IdentityTypes identityTypes;
    private final ReservationService service;
    private final ReportService reports;
    private final MessageRepository messages;
    private final Views views;

    public Api(RoomCatalog rooms, IdentityTypes identityTypes, ReservationService service, ReportService reports,
               MessageRepository messages) {
        this.rooms = rooms;
        this.identityTypes = identityTypes;
        this.service = service;
        this.reports = reports;
        this.messages = messages;
        this.views = new Views(identityTypes);
    }

    public Javalin create() {
        Javalin app = Javalin.create();
        app.get("/rooms", ctx -> json(ctx, 200, rooms.all().stream().map(views::room).toList()));
        app.post("/reservations", this::createReservation);
        app.get("/reservations/{id}", ctx -> json(ctx, 200, views.reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", reservationAction(service::confirm));
        app.post("/reservations/{id}/cancellation", reservationAction(service::cancel));
        app.post("/reservations/{id}/check-in", reservationAction(service::checkIn));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, views.document(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, views.document(service.paidReservation(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, messages.findAll().stream().map(views::message).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(month(ctx.queryParam("month")))));
        app.exception(AppException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno.");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado.");
            }
        });
        return app;
    }

    private io.javalin.http.Handler reservationAction(LongFunction<Reservation> action) {
        return ctx -> json(ctx, 200, views.reservation(action.apply(id(ctx))));
    }

    private void createReservation(Context ctx) {
        Map<String, Object> body = readBody(ctx);
        Reservation r = service.create(
                integer(body, "roomNumber"),
                text(body, "guestName"),
                text(body, "guestEmail"),
                identityTypes.parse(body),
                date(text(body, "checkIn")),
                date(text(body, "checkOut")),
                integer(body, "guests"),
                ratePlan(text(body, "ratePlan")));
        json(ctx, 201, views.reservation(r));
    }

    private Map<String, Object> readBody(Context ctx) {
        try {
            JsonNode node = MAPPER.readTree(ctx.body());
            if (node == null || !node.isObject()) throw AppException.invalid("O corpo precisa ser um objeto JSON.");
            Map<String, Object> body = new LinkedHashMap<>();
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v.isTextual()) body.put(e.getKey(), v.asText());
                else if (v.isIntegralNumber()) body.put(e.getKey(), v.asInt());
                else if (!v.isNull()) body.put(e.getKey(), v.toString());
            });
            return body;
        } catch (JsonProcessingException e) {
            throw AppException.invalid("O corpo não é um JSON válido.");
        }
    }

    private static String text(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof String s) || s.isBlank()) {
            throw AppException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return s.trim();
    }

    private static int integer(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof Integer i)) {
            throw AppException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return i;
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw AppException.invalid("Data inválida (use AAAA-MM-DD): " + value);
        }
    }

    private static YearMonth month(String value) {
        try {
            return YearMonth.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw AppException.invalid("Mês inválido (use AAAA-MM): " + value);
        }
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw AppException.invalid("Plano de tarifa inválido: " + value);
        }
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new AppException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe.");
        }
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        json(ctx, status, body);
    }
}
