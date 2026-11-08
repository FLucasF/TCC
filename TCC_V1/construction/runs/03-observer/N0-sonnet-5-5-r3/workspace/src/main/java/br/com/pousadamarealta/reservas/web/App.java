package br.com.pousadamarealta.reservas.web;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.ErrorCode;
import br.com.pousadamarealta.reservas.service.ReportService;
import br.com.pousadamarealta.reservas.service.ReservationService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

/** Rotas HTTP da API. */
public class App {
    private static final String JSON = "application/json";
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final RequestParser parser = new RequestParser(mapper);

    public App(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, reservations.listRooms()));
        app.post("/reservations", ctx -> json(ctx, 201, reservations.create(parser.newReservation(ctx.body()))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.receipt(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, reservations.listMessages()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(parser.queryDate(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                reports.revenue(parser.queryMonth(ctx.queryParam("month"), "month"))));

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).contentType(JSON).result(write(Map.of("error", "INTERNAL_ERROR",
                    "message", "Erro interno")));
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                ctx.contentType(JSON).result(write(Map.of("error", "NOT_FOUND", "message", "Rota não encontrada")));
            }
        });
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }

    private void json(Context ctx, int status, Object body) {
        ctx.status(status).contentType(JSON).result(write(body));
    }

    private void csv(Context ctx, String body) {
        ctx.status(200).contentType(CSV).result(body);
    }

    private void error(Context ctx, ErrorCode code, String message) {
        ctx.status(statusOf(code)).contentType(JSON).result(write(Map.of("error", code.name(), "message", message)));
    }

    private static int statusOf(ErrorCode code) {
        return switch (code) {
            case INVALID_REQUEST -> 400;
            case ROOM_NOT_FOUND, RESERVATION_NOT_FOUND -> 404;
            case ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED -> 409;
            case CAPACITY_EXCEEDED -> 422;
        };
    }

    private String write(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
