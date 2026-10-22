package com.marealta.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marealta.domain.HotelException;
import com.marealta.persistence.MessageLog;
import com.marealta.report.Reports;
import com.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public final class Api {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final MessageLog messages;
    private final Reports reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RequestParser parser = new RequestParser(mapper);

    public Api(ReservationService service, MessageLog messages, Reports reports) {
        this.service = service;
        this.messages = messages;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create();

        app.exception(HotelException.class, (e, ctx) ->
                error(ctx, e.code().httpStatus(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms()));
        app.post("/reservations", ctx ->
                json(ctx, 201, ReservationView.of(service.create(parser.reservation(ctx.body())))));
        app.get("/reservations/{id}", ctx ->
                json(ctx, 200, ReservationView.of(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx ->
                json(ctx, 200, ReservationView.of(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx ->
                json(ctx, 200, ReservationView.of(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx ->
                json(ctx, 200, ReservationView.of(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx ->
                json(ctx, 200, service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx ->
                json(ctx, 200, service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, 200, messages.all()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                reports.revenue(RequestParser.month(ctx.queryParam("month")))));
        return app;
    }

    private void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
    }

    private void csv(Context ctx, String body) {
        ctx.status(200).contentType(CSV).result(body);
    }

    private void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (Exception e) {
            ctx.status(status).result(code);
        }
    }
}
