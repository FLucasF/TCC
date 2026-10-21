package br.com.marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Main {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void main(String[] args) {
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        String port = System.getenv("PORT");
        createApp(testMode).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    static Javalin createApp(boolean testMode) {
        Outbox outbox = new Outbox();
        Hotel hotel = new Hotel(MessagingSetup.build(outbox, testMode));
        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> json(ctx, 200, hotel.rooms()));
        app.post("/reservations", ctx -> json(ctx, 201,
                ReservationView.of(hotel.reserve(ReservationRequest.parse(MAPPER, ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, ReservationView.of(hotel.find(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx ->
                json(ctx, 200, ReservationView.of(hotel.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx ->
                json(ctx, 200, ReservationView.of(hotel.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx ->
                json(ctx, 200, ReservationView.of(hotel.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, hotel.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, hotel.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.messages()));
        app.get("/reports/occupancy", ctx -> csv(ctx, hotel.occupancyReport(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, hotel.revenueReport(month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) ->
                json(ctx, e.status(), Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            json(ctx, 500, Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private static LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalidRequest("date deve estar no formato AAAA-MM-DD");
        }
    }

    private static YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw DomainException.invalidRequest("month deve estar no formato AAAA-MM");
        }
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }
}
