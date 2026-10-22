package com.pousada.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.error.ApiException;
import com.pousada.service.ReportService;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Routes {

    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;
    private final CreateReservationParser parser;

    public Routes(ReservationService reservations, ReportService reports, ObjectMapper mapper) {
        this.reservations = reservations;
        this.reports = reports;
        this.parser = new CreateReservationParser(mapper, reservations.ratePlans());
    }

    public void register(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> ctx.json(reservations.listRooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(reservations.create(parser.parse(ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.listMessages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(reports.occupancy(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(reports.revenue(month(ctx.queryParam("month")))));
    }

    private String id(Context ctx) {
        return ctx.pathParam("id");
    }

    private LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Parâmetro date inválido, use AAAA-MM-DD");
        }
    }

    private YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Parâmetro month inválido, use AAAA-MM");
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
