package pousada.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.BusinessException;
import pousada.ErrorCode;
import pousada.ReservationService;
import pousada.domain.Reservation;
import pousada.domain.Rooms;
import pousada.report.Reports;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;

    public Routes(ReservationService service) {
        this.service = service;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> ctx.json(Rooms.all().stream().map(Views::room).toList()));

        app.post("/reservations", ctx -> {
            Reservation created = service.create(ReservationRequest.parse(ctx.body()));
            ctx.status(201).json(Views.reservation(created));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(Views.reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(Views.reservation(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> transition(ctx, Reservation::cancel));
        app.post("/reservations/{id}/check-in", ctx -> transition(ctx, Reservation::doCheckIn));
        app.post("/reservations/{id}/check-out", ctx -> {
            Reservation done = service.transition(id(ctx), Reservation::doCheckOut);
            ctx.json(Views.document(done));
        });
        app.get("/reservations/{id}/document", ctx -> {
            Reservation r = service.get(id(ctx));
            r.requirePaid();
            ctx.json(Views.document(r));
        });

        app.get("/messages", ctx -> ctx.json(service.messages().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate night = parse(ctx.queryParam("date"), "date", LocalDate::parse, "AAAA-MM-DD");
            ctx.contentType(CSV).result(Reports.occupancy(service.occupyingNight(night)));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month = parse(ctx.queryParam("month"), "month", YearMonth::parse, "AAAA-MM");
            ctx.contentType(CSV).result(Reports.revenue(
                    service.startingBetween(month.atDay(1), month.atEndOfMonth())));
        });

        app.exception(BusinessException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(),
                e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
    }

    private void transition(Context ctx, UnaryOperator<Reservation> change) {
        ctx.json(Views.reservation(service.transition(id(ctx), change)));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND,
                    "Reserva inexistente: " + ctx.pathParam("id"));
        }
    }

    private static <T> T parse(String value, String name, java.util.function.Function<String, T> parser,
                               String format) {
        try {
            if (value != null) {
                return parser.apply(value);
            }
        } catch (DateTimeParseException ignored) {
            // cai na resposta de formato inválido
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, name + " deve estar no formato " + format);
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
