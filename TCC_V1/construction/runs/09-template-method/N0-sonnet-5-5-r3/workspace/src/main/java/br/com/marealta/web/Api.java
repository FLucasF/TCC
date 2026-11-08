package br.com.marealta.web;

import br.com.marealta.report.Report;
import br.com.marealta.report.ReportCatalog;
import br.com.marealta.report.ReportRenderer;
import br.com.marealta.service.ApiException;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.util.Map;

/** HTTP routes. */
public final class Api {

    private final ReservationService service;
    private final ReportCatalog reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public Api(ReservationService service, ReportCatalog reports) {
        this.service = service;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> {
            cfg.jsonMapper(new JavalinJackson(mapper, false));
            cfg.showJavalinBanner = false;
        });

        app.get("/rooms", ctx -> ctx.json(service.listRooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(
                RequestParser.parseBooking(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(service.listMessages()));

        for (Report report : reports.all()) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = reports.csv(report, ctx.queryParam(report.periodKind().parameter()));
                ctx.contentType(ReportRenderer.CONTENT_TYPE).result(csv);
            });
        }

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private static String id(Context ctx) {
        return ctx.pathParam("id");
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
