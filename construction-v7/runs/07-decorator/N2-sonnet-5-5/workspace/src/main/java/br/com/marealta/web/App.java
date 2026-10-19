package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.messaging.DispatchRules;
import br.com.marealta.messaging.MessageLog;
import br.com.marealta.messaging.Notifier;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationRepository;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.RoomCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.Map;

public final class App {
    private App() {}

    public static Javalin create(boolean testMode) {
        RoomCatalog rooms = new RoomCatalog();
        ReservationRepository repository = new ReservationRepository();
        MessageLog log = new MessageLog();
        Notifier notifier = new Notifier(DispatchRules.forMode(testMode), log);
        ReservationService reservations = new ReservationService(rooms, repository, notifier);
        ReportService reports = new ReportService(rooms, repository);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(DomainException.class, (e, ctx) ->
                ctx.status(e.status()).json(Map.of("error", e.code(), "message", e.getMessage())));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(RoomView::of).toList()));

        app.post("/reservations", ctx -> {
            var created = reservations.create(RequestParser.reservation(ctx.body()));
            ctx.status(201).json(ReservationView.of(created));
        });
        app.get("/reservations/{id}", ctx ->
                ctx.json(ReservationView.of(reservations.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx ->
                ctx.json(ReservationView.of(reservations.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx ->
                ctx.json(ReservationView.of(reservations.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx ->
                ctx.json(ReservationView.of(reservations.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx ->
                ctx.json(reservations.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx ->
                ctx.json(reservations.document(ctx.pathParam("id"))));

        app.get("/messages", ctx -> ctx.json(log.all()));

        app.get("/reports/occupancy", ctx -> ctx
                .contentType("text/csv; charset=utf-8")
                .result(reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> ctx
                .contentType("text/csv; charset=utf-8")
                .result(reports.revenue(RequestParser.month(ctx.queryParam("month")))));

        return app;
    }
}
