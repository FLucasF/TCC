package br.com.marealta.web;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.messaging.MessageStore;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class WebApp {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReservationService service;
    private final ReservationRepository reservations;
    private final MessageStore messages;

    public WebApp(ReservationService service, ReservationRepository reservations, MessageStore messages) {
        this.service = service;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, RoomCatalog.all().stream().map(Views::room).toList()));

        app.post("/reservations", ctx -> json(ctx, 201,
                Views.reservation(service.create(ReservationRequest.parse(mapper, ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Views.reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", reservationAction(service::confirm));
        app.post("/reservations/{id}/cancellation", reservationAction(service::cancel));
        app.post("/reservations/{id}/check-in", reservationAction(service::checkIn));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, Views.receipt(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, Views.receipt(service.document(id(ctx)))));

        app.get("/messages", ctx -> json(ctx, 200, messages.all().stream().map(Views::message).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate night = parseDate(ctx.queryParam("date"));
            csv(ctx, OccupancyReport.render(night, reservations.findOccupying(night)));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month = parseMonth(ctx.queryParam("month"));
            csv(ctx, RevenueReport.render(
                    reservations.findCheckingInBetween(month.atDay(1), month.plusMonths(1).atDay(1))));
        });

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private io.javalin.http.Handler reservationAction(Function<Long, Reservation> action) {
        return ctx -> json(ctx, 200, Views.reservation(action.apply(id(ctx))));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound();
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Parâmetro 'date' deve estar no formato aaaa-mm-dd");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Parâmetro 'month' deve estar no formato aaaa-mm");
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        json(ctx, status, body);
    }

    private void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }
}
