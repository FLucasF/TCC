package br.com.marealta.web;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Room;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.List;
import java.util.Map;

public class Api {

    private static final Map<String, String> NO_BODY = Map.of();

    private final ObjectMapper mapper = new ObjectMapper().enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final ReservationService reservations;
    private final ReportService reports;

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.status());
            json(ctx, Map.of("error", e.code(), "message", e.getMessage()));
        });
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            json(ctx, Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> json(ctx, Room.all().stream().map(Views::room).toList()));

        app.post("/reservations", ctx -> {
            var created = reservations.create(RequestParser.reservation(mapper, ctx.body()));
            ctx.status(201);
            json(ctx, Views.reservation(created));
        });
        app.get("/reservations/{id}", ctx -> json(ctx, Views.reservation(reservations.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, Views.reservation(reservations.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, Views.reservation(reservations.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, Views.reservation(reservations.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, Views.receipt(reservations.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, Views.receipt(reservations.receiptCopy(ctx.pathParam("id")))));

        app.get("/messages", ctx -> json(ctx, reservations.sentMessages().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx ->
                csv(ctx, reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx ->
                csv(ctx, reports.revenue(RequestParser.month(ctx.queryParam("month")))));

        return app;
    }

    private void json(Context ctx, Object body) {
        try {
            ctx.contentType("application/json; charset=utf-8").result(mapper.writeValueAsString(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void csv(Context ctx, String body) {
        ctx.contentType("text/csv; charset=utf-8").result(body);
    }
}
