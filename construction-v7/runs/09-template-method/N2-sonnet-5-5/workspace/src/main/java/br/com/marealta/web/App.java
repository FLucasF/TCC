package br.com.marealta.web;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Room;
import br.com.marealta.report.InvalidPeriodException;
import br.com.marealta.report.ReportRegistry;
import br.com.marealta.report.TourismTaxPolicy;
import br.com.marealta.service.InMemoryOutbox;
import br.com.marealta.service.InMemoryReservationRepository;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.RoomCatalog;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public final class App {
    private App() {}

    public static Javalin create() {
        RoomCatalog rooms = new RoomCatalog();
        InMemoryOutbox outbox = new InMemoryOutbox();
        ReservationService service = new ReservationService(rooms, new InMemoryReservationRepository(), outbox);
        ReportRegistry reports = ReportRegistry.standard(rooms, TourismTaxPolicy.current());

        Javalin app = Javalin.create();

        app.exception(BusinessException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(InvalidPeriodException.class, (e, ctx) -> error(ctx, ErrorCode.INVALID_REQUEST, e.getMessage()));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(App::roomView).toList()));

        app.post("/reservations", ctx -> {
            var reservation = service.create(BookingParser.parse(ctx.body()));
            ctx.status(201).json(ReservationView.of(reservation));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.receipt(id(ctx))));

        app.get("/messages", ctx -> ctx.json(outbox.all()));

        app.get("/reports/{name}", ctx -> {
            var report = reports.find(ctx.pathParam("name")).orElse(null);
            if (report == null) {
                ctx.status(404).json(Map.of("error", "NOT_FOUND", "message", "Relatório inexistente"));
                return;
            }
            String csv = report.generate(ctx.queryParam(report.parameter()), service.all());
            ctx.contentType("text/csv; charset=utf-8").result(csv);
        });

        return app;
    }

    private static Map<String, Object> roomView(Room r) {
        return Map.of("number", r.number(), "type", r.type().name(), "capacity", r.capacity(),
                "nightlyRate", r.nightlyRate());
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente");
        }
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        ctx.status(code.status()).json(Map.of("error", code.name(), "message", message));
    }
}
