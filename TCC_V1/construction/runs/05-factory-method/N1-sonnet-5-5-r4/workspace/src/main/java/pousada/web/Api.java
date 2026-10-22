package pousada.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pousada.domain.ErrorCode;
import pousada.domain.PousadaException;
import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;
import pousada.service.ReportService;
import pousada.service.ReservationService;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Api {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final RequestParser parser = new RequestParser(mapper);
    private final ReservationService reservations;
    private final ReportService reports;

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, RoomCatalog.all().stream().map(Views::room).toList()));

        app.post("/reservations", ctx ->
                json(ctx, 201, Views.reservation(reservations.create(parser.newReservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.reservation(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx ->
                json(ctx, 200, Views.reservation(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx ->
                json(ctx, 200, Views.reservation(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx ->
                json(ctx, 200, Views.reservation(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx ->
                json(ctx, 200, document(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx ->
                json(ctx, 200, document(reservations.requirePaid(id(ctx)))));

        app.get("/messages", ctx -> json(ctx, 200, reservations.messages().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                reports.revenue(RequestParser.month(ctx.queryParam("month")))));

        app.exception(PousadaException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            json(ctx, 500, Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private Map<String, Object> document(Reservation reservation) {
        return Views.document(reservation, reservations.document(reservation));
    }

    private void error(Context ctx, ErrorCode code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code.name());
        body.put("message", message);
        json(ctx, code.httpStatus(), body);
    }

    private void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String content) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(content);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }
}
