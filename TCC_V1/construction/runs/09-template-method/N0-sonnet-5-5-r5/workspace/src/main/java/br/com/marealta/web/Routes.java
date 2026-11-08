package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ReservationService;
import br.com.marealta.report.CsvRenderer;
import br.com.marealta.report.Report;
import br.com.marealta.store.Store;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Map;

public final class Routes {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Routes.class);

    private Routes() {}

    public static void register(Javalin app, ObjectMapper mapper, ReservationService service, Store store,
                                List<Report> reports) {
        app.get("/rooms", ctx -> ctx.json(service.rooms()));

        app.post("/reservations", ctx -> ctx.status(HttpStatus.CREATED)
                .json(service.create(RequestParser.reservation(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));

        app.get("/messages", ctx -> ctx.json(service.messages()));

        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = CsvRenderer.render(store.tx(tx -> report.build(tx, ctx.queryParam(report.param()))));
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            });
        }

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
            }
        });
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw DomainException.reservationNotFound(raw);
        }
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
