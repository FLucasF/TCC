package br.com.marealta.web;

import br.com.marealta.ApiException;
import br.com.marealta.ReservationService;
import br.com.marealta.domain.Rooms;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private Routes() {
    }

    public static Javalin create(ReservationService service) {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> ctx.json(Rooms.all().stream().map(Views::room).toList()));

        app.post("/reservations", ctx -> {
            var created = service.create(RequestParser.newReservation(ctx.body()));
            ctx.status(201).json(Views.reservation(created));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(Views.reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation",
                ctx -> ctx.json(Views.reservation(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation",
                ctx -> ctx.json(Views.reservation(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in",
                ctx -> ctx.json(Views.reservation(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out",
                ctx -> ctx.json(Views.document(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document",
                ctx -> ctx.json(Views.document(service.document(id(ctx)))));

        app.get("/messages", ctx -> ctx.json(service.messages().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV)
                .result(service.occupancyReport(RequestParser.day(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV)
                .result(service.revenueReport(RequestParser.month(ctx.queryParam("month")))));
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound();
        }
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        ctx.status(status).json(body);
    }
}
