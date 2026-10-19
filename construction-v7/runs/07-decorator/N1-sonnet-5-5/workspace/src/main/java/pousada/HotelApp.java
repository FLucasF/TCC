package pousada;

import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.messaging.MessagePipeline;
import pousada.messaging.Outbox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class HotelApp {
    private HotelApp() {
    }

    public static Javalin create(boolean testMode) {
        Outbox outbox = new Outbox(MessagePipeline.standard(testMode));
        ReservationService service = new ReservationService(outbox);
        Reports reports = new Reports(service);

        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> ctx.json(service.rooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(BookingRequest.parse(ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.receipt(id(ctx))));
        app.get("/messages", ctx -> ctx.json(outbox.all()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) -> ctx.status(e.status).json(error(e.code, e.getMessage())));
        return app;
    }

    private static String id(Context ctx) {
        return ctx.pathParam("id");
    }

    private static void csv(Context ctx, String content) {
        ctx.contentType("text/csv; charset=utf-8").result(content);
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalid("Data inválida (use AAAA-MM-DD)");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalid("Mês inválido (use AAAA-MM)");
        }
    }

    private static Map<String, String> error(String code, String message) {
        return Map.of("error", code, "message", message);
    }
}
