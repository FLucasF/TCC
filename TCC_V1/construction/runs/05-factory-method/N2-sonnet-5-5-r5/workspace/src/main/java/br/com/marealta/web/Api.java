package br.com.marealta.web;

import br.com.marealta.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongFunction;

public final class Api {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);

    private final ReservationService reservations;
    private final ReportService reports;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RequestReader reader = new RequestReader(mapper);

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create();
        app.get("/rooms", ctx -> json(ctx, 200, RoomCatalog.all().stream().map(Views.RoomView::of).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                Views.ReservationView.of(reservations.create(reader.newReservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> reservation(ctx, reservations::get));
        app.post("/reservations/{id}/confirmation", ctx -> reservation(ctx, reservations::confirm));
        app.post("/reservations/{id}/cancellation", ctx -> reservation(ctx, reservations::cancel));
        app.post("/reservations/{id}/check-in", ctx -> reservation(ctx, reservations::checkIn));
        app.post("/reservations/{id}/check-out", ctx -> document(ctx, reservations::checkOut));
        app.get("/reservations/{id}/document", ctx -> document(ctx, reservations::document));
        app.get("/messages", ctx -> json(ctx, 200, reservations.sentMessages().stream().map(Views.MessageView::of).toList()));
        app.get("/reports/occupancy", ctx ->
                csv(ctx, reports.occupancy(reader.night(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx ->
                csv(ctx, reports.revenue(reader.month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) ->
                json(ctx, e.code().httpStatus(), new Views.ErrorView(e.code().name(), e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            json(ctx, 500, new Views.ErrorView("INTERNAL_ERROR", "Erro interno"));
        });
        return app;
    }

    private void reservation(Context ctx, LongFunction<Reservation> operation) {
        json(ctx, 200, Views.ReservationView.of(operation.apply(reservationId(ctx))));
    }

    private void document(Context ctx, LongFunction<ReservationDocument> operation) {
        json(ctx, 200, Views.DocumentView.of(operation.apply(reservationId(ctx))));
    }

    private static long reservationId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente");
        }
    }

    private static void csv(Context ctx, CsvReport report) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(report.render());
    }

    private void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
