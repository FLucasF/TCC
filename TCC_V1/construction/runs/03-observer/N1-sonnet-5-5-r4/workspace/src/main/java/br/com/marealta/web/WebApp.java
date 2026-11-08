package br.com.marealta.web;

import br.com.marealta.app.MessageRepository;
import br.com.marealta.app.ReservationService;
import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.RoomCatalog;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.Function;

public class WebApp {
    private static final Logger log = LoggerFactory.getLogger(WebApp.class);
    private static final String CSV = "text/csv; charset=utf-8";

    private final RoomCatalog rooms;
    private final ReservationService service;
    private final MessageRepository messages;
    private final OccupancyReport occupancy;
    private final RevenueReport revenue;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final ReservationRequestParser parser = new ReservationRequestParser(mapper);

    public WebApp(RoomCatalog rooms, ReservationService service, MessageRepository messages,
                  OccupancyReport occupancy, RevenueReport revenue) {
        this.rooms = rooms;
        this.service = service;
        this.messages = messages;
        this.occupancy = occupancy;
        this.revenue = revenue;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(RoomView::of).toList()));

        app.post("/reservations", ctx -> ctx.status(201).json(service.create(parser.parse(ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));

        app.get("/messages", ctx -> ctx.json(messages.findAll()));

        app.get("/reports/occupancy", ctx -> csv(ctx, "date", LocalDate::parse, occupancy::generate));
        app.get("/reports/revenue", ctx -> csv(ctx, "month", YearMonth::parse, revenue::generate));

        app.exception(DomainException.class, (e, ctx) ->
                ctx.status(e.code().httpStatus()).json(Map.of("error", e.code().name(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Erro inesperado", e);
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }

    private static <T> void csv(Context ctx, String param, Function<String, T> parse, Function<T, String> report) {
        String raw = ctx.queryParam(param);
        if (raw == null) {
            throw ReservationRequestParser.invalid("Parâmetro " + param + " é obrigatório");
        }
        T value;
        try {
            value = parse.apply(raw);
        } catch (DateTimeParseException e) {
            throw ReservationRequestParser.invalid("Parâmetro " + param + " em formato inválido");
        }
        ctx.contentType(CSV).result(report.apply(value));
    }
}
