package pousada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    public static Javalin create() {
        RoomCatalog rooms = new RoomCatalog();
        Outbox outbox = new Outbox();
        IdentityRegistry identities = new IdentityRegistry();
        ReservationService service = new ReservationService(rooms, outbox);
        Views views = new Views(identities);

        Javalin app = Javalin.create();
        app.exception(ApiException.class, (e, ctx) -> {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.code());
            error.put("message", e.getMessage());
            json(ctx.status(e.status()), error);
        });

        app.get("/rooms", ctx -> json(ctx, rooms.all().stream().map(views::room).toList()));

        app.post("/reservations", ctx -> {
            NewReservation request = NewReservation.from(RequestBody.parse(ctx.body()), identities);
            json(ctx.status(201), views.reservation(service.create(request)));
        });
        app.get("/reservations/{id}", ctx -> json(ctx, views.reservation(service.find(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, views.reservation(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, views.reservation(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, views.reservation(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, views.document(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, views.document(service.document(ctx.pathParam("id")))));

        app.get("/messages", ctx -> json(ctx, outbox.all().stream().map(views::message).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"));
            csv(ctx, new OccupancyReport(service.occupying(date), rooms.count()));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month = parseMonth(ctx.queryParam("month"));
            csv(ctx, new RevenueReport(service.retainedStartingIn(month)));
        });
        return app;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("O parâmetro date deve ter o formato 2027-07-10");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value == null ? "" : value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("O parâmetro month deve ter o formato 2027-07");
        }
    }

    private static void csv(Context ctx, CsvReport report) {
        ctx.contentType("text/csv; charset=utf-8").result(report.render());
    }

    private static void json(Context ctx, Object body) {
        try {
            ctx.contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
