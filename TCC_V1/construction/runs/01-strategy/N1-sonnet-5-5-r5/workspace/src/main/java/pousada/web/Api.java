package pousada.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import pousada.domain.DomainException;
import pousada.domain.Rooms;
import pousada.service.ReservationService;

import java.util.Map;

public class Api {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public Api(ReservationService service) {
        this.service = service;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(Rooms.all()));
        app.post("/reservations", ctx ->
                ctx.status(201).json(service.create(RequestParser.createReservation(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(service.messages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(
                service.occupancyReport(RequestParser.date("date", ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(
                service.revenueReport(RequestParser.month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno.");
        });
        return app;
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe.");
        }
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
