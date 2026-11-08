package br.com.marealta.web;

import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.report.Report;
import br.com.marealta.report.ReportService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private App() {
    }

    public static Javalin create(ReservationService reservations, ReportService reports) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        RequestParser parser = new RequestParser(mapper);

        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(PousadaException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unexpected error", e);
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Unexpected error"));
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                ctx.json(Map.of("error", "NOT_FOUND", "message", "Resource not found"));
            }
        });

        app.get("/rooms", ctx -> ctx.json(reservations.rooms()));
        app.post("/reservations", ctx ->
                ctx.status(201).json(reservations.create(parser.reservation(ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.messages()));

        for (Report report : reports.reports()) {
            app.get("/reports/" + report.name(), ctx -> {
                String period = ctx.queryParam(report.parameter());
                if (period == null || period.isBlank()) {
                    throw new PousadaException(ErrorCode.INVALID_REQUEST,
                            "Query parameter " + report.parameter() + " is required");
                }
                ctx.contentType("text/csv; charset=utf-8").result(reports.csv(report, period));
            });
        }
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found");
        }
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        ctx.status(code.httpStatus()).json(Map.of("error", code.name(), "message", message));
    }
}
