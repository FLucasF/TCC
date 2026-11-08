package br.com.marealta.web;

import br.com.marealta.DomainException;
import br.com.marealta.domain.Rooms;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class Routes {
    private final ReservationService reservations;
    private final ReportService reports;
    private final ObjectMapper mapper;

    public Routes(ReservationService reservations, ReportService reports, ObjectMapper mapper) {
        this.reservations = reservations;
        this.reports = reports;
        this.mapper = mapper;
    }

    public void register(Javalin app) {
        app.exception(DomainException.class, (e, ctx) ->
                ctx.status(e.status()).json(Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(Rooms.ALL));
        app.post("/reservations", ctx ->
                ctx.status(201).json(reservations.create(RequestParser.reservation(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.messages()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                reports.revenue(RequestParser.month(ctx.queryParam("month")))));
    }

    private static void csv(Context ctx, String body) {
        ctx.contentType("text/csv; charset=utf-8").result(body);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }
}
