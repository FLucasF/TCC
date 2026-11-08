package pousada;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import pousada.report.Report;

import java.util.List;
import java.util.Map;

public final class Api {
    private Api() {
    }

    public static Javalin create(ReservationService service, List<Report<?>> reports) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(ApiException.class, (e, ctx) -> ctx.status(e.status()).json(error(e.code(), e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(error("INTERNAL_ERROR", "Erro interno"));
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                ctx.json(error("NOT_FOUND", "Recurso não encontrado"));
            }
        });

        app.get("/rooms", ctx -> ctx.json(service.rooms()));
        app.post("/reservations", ctx -> {
            ReservationRequest request = ReservationRequest.parse(readBody(mapper, ctx.body()));
            ctx.status(201).json(service.create(request));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(ctx.pathParam("id"))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(ctx.pathParam("id"))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> ctx.json(service.messages()));

        for (Report<?> report : reports) {
            app.get("/reports/" + report.name(), ctx -> ctx
                    .contentType("text/csv; charset=utf-8")
                    .result(report.generate(ctx.queryParam(report.periodParam()))));
        }
        return app;
    }

    private static JsonNode readBody(ObjectMapper mapper, String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
    }

    private static Map<String, String> error(String code, String message) {
        return Map.of("error", code, "message", message);
    }
}
