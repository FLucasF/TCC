package br.com.marealta.pousada.web;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import br.com.marealta.pousada.domain.DomainException;
import br.com.marealta.pousada.domain.ReservationService;
import br.com.marealta.pousada.domain.RoomCatalog;
import br.com.marealta.pousada.notification.MessageLog;
import br.com.marealta.pousada.report.OccupancyReport;
import br.com.marealta.pousada.report.RevenueReport;

import static br.com.marealta.pousada.domain.DomainException.Kind.RESERVATION_NOT_FOUND;

public final class Api {

    private final ReservationService service;
    private final RoomCatalog rooms;
    private final MessageLog messages;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final RequestParser parser = new RequestParser(mapper);

    public Api(ReservationService service, RoomCatalog rooms, MessageLog messages) {
        this.service = service;
        this.rooms = rooms;
        this.messages = messages;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.kind().httpStatus(), e.kind().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, rooms.all().stream().map(RoomView::of).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                ReservationView.of(service.create(parser.reservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, messages.all()));
        app.get("/reports/occupancy", ctx -> csv(ctx, OccupancyReport.render(
                parser.date(ctx.queryParam("date"), "date"), service.all(), rooms)));
        app.get("/reports/revenue", ctx -> csv(ctx, RevenueReport.render(
                parser.month(ctx.queryParam("month")), service.all())));
        return app;
    }

    private long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(RESERVATION_NOT_FOUND, "reserva inexistente");
        }
    }

    private void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
    }

    private void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (Exception e) {
            ctx.status(500);
        }
    }
}
