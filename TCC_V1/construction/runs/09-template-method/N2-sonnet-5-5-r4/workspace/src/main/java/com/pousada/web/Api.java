package com.pousada.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.RoomCatalog;
import com.pousada.report.Report;
import com.pousada.report.ReportRegistry;
import com.pousada.repository.MessageRepository;
import com.pousada.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.util.Map;

public class Api {
    private final RoomCatalog rooms;
    private final ReservationService service;
    private final MessageRepository messages;
    private final ReportRegistry reports;
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();

    public Api(RoomCatalog rooms, ReservationService service, MessageRepository messages, ReportRegistry reports) {
        this.rooms = rooms;
        this.service = service;
        this.messages = messages;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(rooms.all()));
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(RequestParser.parse(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(messages.findAll()));
        app.get("/reports/{name}", this::report);

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, ErrorCode.NOT_FOUND, "Recurso não encontrado");
            }
        });
        return app;
    }

    private void report(Context ctx) {
        Report<?> report = reports.find(ctx.pathParam("name"))
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Relatório não existe"));
        ctx.contentType("text/csv; charset=utf-8").result(report.render(ctx.queryParam(report.parameter())));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        ctx.status(code.httpStatus()).json(Map.of("error", code.name(), "message", message));
    }
}
