package br.com.marealta.reservas.api;

import br.com.marealta.reservas.domain.DomainException;
import br.com.marealta.reservas.domain.ErrorCode;
import br.com.marealta.reservas.notification.Message;
import br.com.marealta.reservas.persistence.MessageRepository;
import br.com.marealta.reservas.report.Reports;
import br.com.marealta.reservas.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

public class Api {
    private final ReservationService service;
    private final Reports reports;
    private final MessageRepository messages;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RequestParser parser = new RequestParser(mapper);

    public Api(ReservationService service, Reports reports, MessageRepository messages) {
        this.service = service;
        this.reports = reports;
        this.messages = messages;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            send(ctx, 500, new Views.ErrorView("INTERNAL_ERROR", "Erro interno"));
        });

        app.get("/rooms", ctx -> send(ctx, 200, service.rooms().stream().map(Views.RoomView::of).toList()));
        app.post("/reservations", ctx -> send(ctx, 201,
                Views.ReservationView.of(service.create(parser.newReservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> send(ctx, 200, Views.ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx ->
                send(ctx, 200, Views.ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx ->
                send(ctx, 200, Views.ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx ->
                send(ctx, 200, Views.ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx ->
                send(ctx, 200, Views.ReceiptView.of(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx ->
                send(ctx, 200, Views.ReceiptView.of(service.document(id(ctx)))));
        app.get("/messages", ctx -> {
            List<Message> all = messages.findAll();
            send(ctx, 200, all);
        });
        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(parser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parser.month(ctx.queryParam("month")))));
        return app;
    }

    private long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada");
        }
    }

    private void error(Context ctx, ErrorCode code, String message) {
        send(ctx, code.httpStatus(), new Views.ErrorView(code.name(), message));
    }

    private void send(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json; charset=utf-8")
                    .result(mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void csv(Context ctx, String content) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(content);
    }
}
