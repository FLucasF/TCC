package marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static Javalin create() {
        Outbox outbox = new Outbox();
        ReservationService service = new ReservationService(outbox);
        RatePlans plans = RatePlans.defaults();
        Reports reports = new Reports(service);

        Javalin app = Javalin.create();
        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(RoomView::of).toList()));
        app.post("/reservations", ctx -> {
            var request = ReservationRequest.parse(ctx.body(), MAPPER, plans);
            json(ctx, 201, ReservationView.of(service.create(request)));
        });
        app.get("/reservations/{id}", ctx -> json(ctx, 200, ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.sent()));
        app.get("/reports/occupancy", ctx -> {
            LocalDate date;
            try {
                date = LocalDate.parse(String.valueOf(ctx.queryParam("date")));
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro date deve estar no formato AAAA-MM-DD");
            }
            csv(ctx, reports.occupancy(date));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month;
            try {
                month = YearMonth.parse(String.valueOf(ctx.queryParam("month")));
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro month deve estar no formato AAAA-MM");
            }
            csv(ctx, reports.revenue(month));
        });

        app.exception(ApiException.class, (e, ctx) ->
                json(ctx, e.status(), Map.of("error", e.code(), "message", e.getMessage())));
        return app;
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.contentType("text/csv; charset=utf-8").result(body);
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        create().start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port));
    }
}
