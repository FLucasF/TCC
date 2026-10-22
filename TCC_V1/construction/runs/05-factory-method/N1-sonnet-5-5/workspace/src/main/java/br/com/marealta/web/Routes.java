package br.com.marealta.web;

import br.com.marealta.domain.AppException;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.Function;

public final class Routes {
    private static final Logger LOG = LoggerFactory.getLogger(Routes.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;

    public Routes(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public void register(Javalin app) {
        app.exception(AppException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, reservations.listRooms().stream().map(Views::room).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                Views.reservation(reservations.create(ReservationRequestParser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.reservation(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", reservation(reservations::confirm));
        app.post("/reservations/{id}/cancellation", reservation(reservations::cancel));
        app.post("/reservations/{id}/check-in", reservation(reservations::checkIn));
        app.post("/reservations/{id}/check-out", ctx ->
                json(ctx, 200, Views.document(reservations.checkOut(id(ctx)).document())));
        app.get("/reservations/{id}/document", ctx ->
                json(ctx, 200, Views.document(reservations.duplicate(id(ctx)).document())));
        app.get("/messages", ctx -> json(ctx, 200, reservations.listMessages().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(
                parseParam(ctx, "date", LocalDate::parse))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(
                parseParam(ctx, "month", YearMonth::parse))));
    }

    private io.javalin.http.Handler reservation(Function<Long, br.com.marealta.domain.Reservation> action) {
        return ctx -> json(ctx, 200, Views.reservation(action.apply(id(ctx))));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new AppException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }

    private static <T> T parseParam(Context ctx, String name, Function<String, T> parser) {
        String value = ctx.queryParam(name);
        if (value == null) {
            throw AppException.invalid("O parâmetro " + name + " é obrigatório");
        }
        try {
            return parser.apply(value);
        } catch (DateTimeParseException e) {
            throw AppException.invalid("O parâmetro " + name + " está em formato inválido");
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType(CSV).result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        json(ctx, status, Map.of("error", code, "message", message));
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
