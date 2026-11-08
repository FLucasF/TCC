package br.com.mare.web;

import br.com.mare.domain.ApiException;
import br.com.mare.domain.ReportService;
import br.com.mare.domain.ReservationService;
import br.com.mare.domain.Rooms;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public final class Routes {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Routes() {}

    public static Javalin register(Javalin app, ReservationService service, ReportService reports) {
        app.get("/rooms", ctx -> json(ctx, 200, Rooms.all().stream().map(Views::room).toList()));
        app.post("/reservations", ctx ->
                json(ctx, 201, Views.reservation(service.create(RequestParser.reservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, Views.reservation(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, Views.reservation(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, Views.reservation(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, Views.receipt(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, Views.receipt(service.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, service.messages().stream().map(Views::message).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(RequestParser.month(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private static int id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound("Reserva " + raw + " não existe");
        }
    }

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message == null ? "" : message));
        } catch (Exception e) {
            ctx.status(status);
        }
    }
}
