package br.com.marealta.web;

import br.com.marealta.db.Database;
import br.com.marealta.message.MessageRepository;
import br.com.marealta.reservation.ReportService;
import br.com.marealta.reservation.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Routes {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageRepository messages;
    private final Database database;

    public Routes(ReservationService reservations, ReportService reports, MessageRepository messages,
                  Database database) {
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
        this.database = database;
    }

    public void register(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
            }
        });

        app.get("/rooms", ctx -> json(ctx, 200, reservations.rooms().stream().map(Views::room).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, Views.reservation(
                reservations.create(ReservationRequestParser.parse(MAPPER, ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.reservation(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, Views.reservation(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, Views.reservation(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, Views.reservation(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, Views.receipt(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, Views.receipt(reservations.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200,
                database.run(messages::all).stream().map(Views::message).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(month(ctx.queryParam("month")))));
    }

    private static String id(Context ctx) {
        return ctx.pathParam("id");
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida, use o formato 2027-07-10");
        }
    }

    private static YearMonth month(String value) {
        try {
            return YearMonth.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Mês inválido, use o formato 2027-07");
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

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json; charset=utf-8").result(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
