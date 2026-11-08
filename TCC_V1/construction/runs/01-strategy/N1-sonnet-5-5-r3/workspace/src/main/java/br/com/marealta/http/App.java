package br.com.marealta.http;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.DomainException.Code;
import br.com.marealta.domain.Rooms;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReservationService reservations;
    private final ReportService reports;

    public App(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, Rooms.ALL.stream().map(Json::room).toList()));

        app.post("/reservations", ctx -> json(ctx, 201,
                Json.reservation(reservations.create(RequestParser.newReservation(MAPPER, ctx.body())))));
        app.get("/reservations/{id}", ctx -> reservation(ctx, reservations::get));
        app.post("/reservations/{id}/confirmation", ctx -> reservation(ctx, reservations::confirm));
        app.post("/reservations/{id}/cancellation", ctx -> reservation(ctx, reservations::cancel));
        app.post("/reservations/{id}/check-in", ctx -> reservation(ctx, reservations::checkIn));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, Json.receipt(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, Json.receipt(reservations.document(id(ctx)))));

        app.get("/messages", ctx -> json(ctx, 200, reservations.messages().stream().map(Json::message).toList()));

        app.get("/reports/occupancy", ctx -> csv(ctx,
                reports.occupancy(RequestParser.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                reports.revenue(RequestParser.month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) ->
                json(ctx, e.code().httpStatus(), Json.error(e.code().name(), e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            json(ctx, 500, Json.error("INTERNAL_ERROR", "Erro interno"));
        });
        return app;
    }

    private void reservation(Context ctx, Function<Long, br.com.marealta.domain.Reservation> action) {
        json(ctx, 200, Json.reservation(action.apply(id(ctx))));
    }

    /** Id que não é número não pode existir. */
    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new DomainException(Code.RESERVATION_NOT_FOUND, "Reserva não encontrada: " + raw);
        }
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }
}
