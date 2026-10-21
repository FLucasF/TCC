package br.com.marealta.web;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.notification.FinanceNotifier;
import br.com.marealta.notification.GuestConfirmationEmail;
import br.com.marealta.notification.HousekeepingNotifier;
import br.com.marealta.notification.MessageOutbox;
import br.com.marealta.notification.ReservationEvents;
import br.com.marealta.notification.ReservationListener;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.RoomCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public final class App {
    private static final String CSV = "text/csv; charset=utf-8";

    private final RoomCatalog rooms = new RoomCatalog();
    private final MessageOutbox outbox = new MessageOutbox();
    private final ReservationService reservations;
    private final OccupancyReport occupancy;
    private final RevenueReport revenue;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public App() {
        // Quem é avisado das reservas: para incluir ou tirar alguém, mexa só nesta lista.
        List<ReservationListener> listeners = List.of(
                new GuestConfirmationEmail(outbox),
                new HousekeepingNotifier(outbox),
                new FinanceNotifier(outbox));
        reservations = new ReservationService(rooms, new ReservationEvents(listeners));
        occupancy = new OccupancyReport(reservations, rooms);
        revenue = new RevenueReport(reservations);
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(mapper, false));
        });

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(RoomResponse::of).toList()));

        app.post("/reservations", ctx -> ctx.status(201).json(ReservationResponse.of(
                reservations.create(RequestBody.parseNewReservation(mapper, ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationResponse.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation",
                ctx -> ctx.json(ReservationResponse.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation",
                ctx -> ctx.json(ReservationResponse.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in",
                ctx -> ctx.json(ReservationResponse.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.receipt(id(ctx))));

        app.get("/messages", ctx -> ctx.json(outbox.sent()));

        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(occupancy.generate(date(ctx, "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(revenue.generate(month(ctx, "month"))));

        app.exception(DomainException.class, (e, ctx) ->
                ctx.status(e.code().httpStatus()).json(Map.of("error", e.code().name(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe: " + ctx.pathParam("id"));
        }
    }

    private static LocalDate date(Context ctx, String param) {
        try {
            return LocalDate.parse(String.valueOf(ctx.queryParam(param)));
        } catch (DateTimeParseException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Parâmetro " + param + " inválido (use 2027-07-10)");
        }
    }

    private static YearMonth month(Context ctx, String param) {
        try {
            return YearMonth.parse(String.valueOf(ctx.queryParam(param)));
        } catch (DateTimeParseException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Parâmetro " + param + " inválido (use 2027-07)");
        }
    }
}
