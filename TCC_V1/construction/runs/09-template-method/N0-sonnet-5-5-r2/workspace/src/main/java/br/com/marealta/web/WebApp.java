package br.com.marealta.web;

import br.com.marealta.report.CsvWriter;
import br.com.marealta.report.Report;
import br.com.marealta.service.ApiException;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebApp {
    private static final Logger LOG = LoggerFactory.getLogger(WebApp.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private WebApp() {}

    public static Javalin create(ReservationService service, List<Report> reports) {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(Views::of).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                Views.of(service.create(RequestParser.newReservation(MAPPER, ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.of(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, Views.of(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, Views.of(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, Views.of(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, 200, service.messages()));

        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = CsvWriter.render(report.generate(ctx.queryParam(report.parameter())));
                ctx.status(200).contentType(CsvWriter.CONTENT_TYPE).result(csv.getBytes(StandardCharsets.UTF_8));
            });
        }

        app.exception(ApiException.class, (e, ctx) ->
                json(ctx, e.status(), new Views.ErrorView(e.code(), e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado em {} {}", ctx.method(), ctx.path(), e);
            json(ctx, 500, new Views.ErrorView("INTERNAL_ERROR", "Erro interno"));
        });
        return app;
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json")
                    .result(MAPPER.writeValueAsBytes(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
