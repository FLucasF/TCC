package br.com.marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.util.Map;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ReservationService reservations;
    private final ReportService reports;

    public Routes(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin build() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(Rooms.all()));
        app.post("/reservations", ctx -> ctx.status(201)
                .json(reservations.create(NewReservation.parse(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.messages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV)
                .result(reports.occupancy(ctx.queryParam("date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV)
                .result(reports.revenue(ctx.queryParam("month"))));

        app.exception(ApiError.class, (e, ctx) -> ctx.status(e.status())
                .json(Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private static String id(Context ctx) {
        return ctx.pathParam("id");
    }
}
