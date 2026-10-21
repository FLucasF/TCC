package br.com.marealta.pousada.web;

import br.com.marealta.pousada.ReservationService;
import br.com.marealta.pousada.domain.*;
import br.com.marealta.pousada.events.Outbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.Function;

public class Api {
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                                  LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                  int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                                  ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(),
                    r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
        }
    }

    private final ReservationService service;
    private final Outbox outbox;

    public Api(ReservationService service, Outbox outbox) {
        this.service = service;
        this.outbox = outbox;
    }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);
        app.get("/rooms", ctx -> json(ctx, 200, service.rooms()));
        app.post("/reservations", ctx -> json(ctx, 201, ReservationView.of(service.create(parse(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(ctx, service::get)));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(ctx, service::confirm)));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(ctx, service::cancel)));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(ctx, service::checkIn)));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.all()));
        app.get("/reports/occupancy", ctx -> csv(ctx, service.occupancyCsv(date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, service.revenueCsv(month(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            String type = ctx.contentType();
            if (type == null || !type.startsWith("application/json")) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
            }
        });
        return app;
    }

    private static ReservationView view(Context ctx, Function<String, Reservation> op) {
        return ReservationView.of(op.apply(ctx.pathParam("id")));
    }

    private static void json(Context ctx, int status, Object body) throws JsonProcessingException {
        ctx.status(status).contentType("application/json").result(JSON.writeValueAsBytes(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body.getBytes(StandardCharsets.UTF_8));
    }

    private static void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (JsonProcessingException e) {
            ctx.status(status).result(code);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(400, "INVALID_REQUEST", message);
    }

    private static LocalDate date(String value, String name) {
        if (value == null) throw invalid("Parâmetro " + name + " é obrigatório");
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + name + ": use AAAA-MM-DD");
        }
    }

    private static YearMonth month(String value) {
        if (value == null) throw invalid("Parâmetro month é obrigatório");
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Mês inválido: use AAAA-MM");
        }
    }

    private static ReservationService.NewReservation parse(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw invalid("Corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw invalid("Corpo deve ser um objeto JSON");
        int room = integer(body, "roomNumber");
        int guests = integer(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan inválido");
        }
        return new ReservationService.NewReservation(room, text(body, "guestName"), text(body, "guestEmail"),
                text(body, "guestCpf"), date(text(body, "checkIn"), "checkIn"),
                date(text(body, "checkOut"), "checkOut"), guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) throw invalid("Campo " + field + " é obrigatório");
        return n.asText().trim();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw invalid("Campo " + field + " deve ser um número inteiro");
        }
        return n.asInt();
    }
}
