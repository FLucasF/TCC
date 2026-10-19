package br.com.mareal.api;

import br.com.mareal.domain.ApiException;
import br.com.mareal.domain.Outbox;
import br.com.mareal.domain.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import java.util.Map;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private Routes() {}

    public static Javalin create(ReservationService service, Outbox outbox) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            config.jsonMapper(new JavalinJackson(mapper, false));
        });

        app.exception(ApiException.class, (e, ctx) -> ctx.status(e.status)
                .json(Map.of("error", e.code, "message", e.getMessage())));

        app.get("/rooms", ctx -> ctx.json(service.rooms().stream()
                .map(r -> Map.of("number", r.number(), "type", r.type().name(),
                        "capacity", r.capacity(), "nightlyRate", r.nightlyRate()))
                .toList()));

        app.post("/reservations", ctx -> ctx.status(201)
                .json(ReservationJson.of(service.create(RequestParser.reservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationJson.of(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> ctx.json(ReservationJson.of(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> ctx.json(ReservationJson.of(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> ctx.json(ReservationJson.of(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(ctx.pathParam("id"))));

        app.get("/messages", ctx -> ctx.json(outbox.sent()));

        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV)
                .result(service.occupancyReport(RequestParser.day(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV)
                .result(service.revenueReport(RequestParser.month(ctx.queryParam("month")))));
        return app;
    }
}
