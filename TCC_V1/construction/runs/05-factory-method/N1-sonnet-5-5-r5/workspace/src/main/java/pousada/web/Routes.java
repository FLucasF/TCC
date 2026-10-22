package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pousada.ApiException;
import pousada.domain.Rooms;
import pousada.service.ReportService;
import pousada.service.ReservationService;

public final class Routes {
    private static final Logger LOG = LoggerFactory.getLogger(Routes.class);
    private static final String CSV = "text/csv; charset=utf-8";

    private final ObjectMapper json = new ObjectMapper();
    private final RequestParser parser = new RequestParser(json);
    private final ReservationService reservations;
    private final ReportService reports;

    public Routes(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> respond(ctx, 200, Rooms.all().stream().map(Views::room).toList()));

        app.post("/reservations", ctx ->
                respond(ctx, 201, Views.reservation(reservations.create(parser.reservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> reservation(ctx, reservations::get));
        app.post("/reservations/{id}/confirmation", ctx -> reservation(ctx, reservations::confirm));
        app.post("/reservations/{id}/cancellation", ctx -> reservation(ctx, reservations::cancel));
        app.post("/reservations/{id}/check-in", ctx -> reservation(ctx, reservations::checkIn));
        app.post("/reservations/{id}/check-out", ctx ->
                respond(ctx, 200, Views.document(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx ->
                respond(ctx, 200, Views.document(reservations.document(id(ctx)))));

        app.get("/messages", ctx ->
                respond(ctx, 200, reservations.sentMessages().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                reports.revenue(RequestParser.month(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) ->
                error(ctx, e.code().status, e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
    }

    private void reservation(Context ctx, Function<Long, pousada.domain.Reservation> operation) {
        respond(ctx, 200, Views.reservation(operation.apply(id(ctx))));
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(ApiException.Code.RESERVATION_NOT_FOUND, "Reserva " + raw + " não existe");
        }
    }

    private static void csv(Context ctx, String content) {
        ctx.status(200).contentType(CSV).result(content);
    }

    private void error(Context ctx, int status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        respond(ctx, status, body);
    }

    private void respond(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(json.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
