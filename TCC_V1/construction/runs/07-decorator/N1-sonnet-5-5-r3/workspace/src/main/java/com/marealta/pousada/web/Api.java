package com.marealta.pousada.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marealta.pousada.domain.*;
import com.marealta.pousada.mail.MailService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class Api {
    private final ReservationService reservations;
    private final ReportService reports;
    private final MailService mail;
    private final RoomCatalog rooms;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public Api(ReservationService reservations, ReportService reports, MailService mail, RoomCatalog rooms) {
        this.reservations = reservations;
        this.reports = reports;
        this.mail = mail;
        this.rooms = rooms;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(DomainException.class, (e, ctx) ->
                ctx.status(e.code().httpStatus()).json(Map.of("error", e.code().name(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(RoomResponse::of).toList()));
        app.post("/reservations", ctx -> ctx.status(201)
                .json(ReservationResponse.of(reservations.create(NewReservationParser.parse(ctx.body(), mapper)))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationResponse.of(reservations.find(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationResponse.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationResponse.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationResponse.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(mail.sent().stream().map(MessageResponse::of).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));
        return app;
    }

    private static void csv(Context ctx, String content) {
        ctx.contentType("text/csv; charset=utf-8").result(content);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada");
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Data inválida; use o formato 2027-07-10");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Mês inválido; use o formato 2027-07");
        }
    }
}
