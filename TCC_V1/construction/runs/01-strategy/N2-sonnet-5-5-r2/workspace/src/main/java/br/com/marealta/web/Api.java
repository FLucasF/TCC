package br.com.marealta.web;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Rooms;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.Function;

public class Api {
    private final ObjectMapper mapper = new ObjectMapper();
    private final BookingParser bookingParser = new BookingParser(mapper);
    private final ReservationService service;
    private final ReportService reports;

    public Api(ReservationService service, ReportService reports) {
        this.service = service;
        this.reports = reports;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, Rooms.all().stream().map(Views::room).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, Views.reservation(service.create(bookingParser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, Views.reservation(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, Views.reservation(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, Views.reservation(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, Views.receipt(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, Views.receipt(service.receipt(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, service.sentMessages().stream().map(Views::message).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(
                parse(ctx.queryParam("date"), "date", LocalDate::parse))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(
                parse(ctx.queryParam("month"), "month", YearMonth::parse))));

        app.exception(BusinessException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).contentType("application/json; charset=utf-8")
                    .result(write(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno")));
        });
        return app.start(port);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada: " + ctx.pathParam("id"));
        }
    }

    private static <T> T parse(String value, String name, Function<String, T> parser) {
        if (value == null) {
            throw BusinessException.invalid("Parâmetro obrigatório: " + name);
        }
        try {
            return parser.apply(value);
        } catch (DateTimeParseException e) {
            throw BusinessException.invalid("Valor inválido em " + name + ": " + value);
        }
    }

    private void json(Context ctx, int status, Object body) {
        ctx.status(status).contentType("application/json; charset=utf-8").result(write(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private void error(Context ctx, ErrorCode code, String message) {
        json(ctx, code.httpStatus(), Map.of("error", code.name(), "message", message));
    }

    private String write(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
