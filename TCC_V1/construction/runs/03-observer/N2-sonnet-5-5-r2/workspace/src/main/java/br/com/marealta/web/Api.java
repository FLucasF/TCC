package br.com.marealta.web;

import br.com.marealta.app.ReportService;
import br.com.marealta.app.ReservationService;
import br.com.marealta.app.RoomCatalog;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.persistence.MessageStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** As rotas HTTP que o site usa. */
public final class Api {

    private static final Map<ErrorCode, Integer> STATUS = Map.of(
            ErrorCode.INVALID_REQUEST, 400,
            ErrorCode.ROOM_NOT_FOUND, 404,
            ErrorCode.RESERVATION_NOT_FOUND, 404,
            ErrorCode.ROOM_UNAVAILABLE, 409,
            ErrorCode.OPERATION_NOT_ALLOWED, 409,
            ErrorCode.CAPACITY_EXCEEDED, 422);

    private final RoomCatalog rooms;
    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageStore messages;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ReservationRequestParser parser = new ReservationRequestParser(mapper);

    public Api(RoomCatalog rooms, ReservationService reservations, ReportService reports, MessageStore messages) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(PousadaException.class, (e, ctx) -> ctx.status(STATUS.get(e.code()))
                .json(Map.of("error", e.code().name(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(Views.RoomView::of).toList()));

        app.post("/reservations", ctx ->
                ctx.status(201).json(Views.ReservationView.of(reservations.create(parser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(Views.ReservationView.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation",
                ctx -> ctx.json(Views.ReservationView.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation",
                ctx -> ctx.json(Views.ReservationView.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in",
                ctx -> ctx.json(Views.ReservationView.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));

        app.get("/messages", ctx -> ctx.json(messages.findAll()));

        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(dateParam(ctx, "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(monthParam(ctx, "month"))));
        return app;
    }

    private static void csv(Context ctx, String content) {
        ctx.contentType("text/csv; charset=utf-8").result(content);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "A reserva não existe");
        }
    }

    private static LocalDate dateParam(Context ctx, String name) {
        try {
            return LocalDate.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "O parâmetro " + name + " deve ser AAAA-MM-DD");
        }
    }

    private static YearMonth monthParam(Context ctx, String name) {
        String value = required(ctx, name);
        if (!value.matches("\\d{4}-\\d{2}")) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "O parâmetro " + name + " deve ser AAAA-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "O parâmetro " + name + " deve ser AAAA-MM");
        }
    }

    private static String required(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "O parâmetro " + name + " é obrigatório");
        }
        return value;
    }
}
