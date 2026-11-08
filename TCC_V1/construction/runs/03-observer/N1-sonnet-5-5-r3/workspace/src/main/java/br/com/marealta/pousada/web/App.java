package br.com.marealta.pousada.web;

import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.service.ServiceException;
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

public class App {

    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;
    private final ReservationRequestReader reader = new ReservationRequestReader();

    public App(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(reservations.rooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(reservations.create(reader.read(ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.receipt(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.sentMessages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(reports.occupancy(dateParam(ctx, "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(reports.revenue(monthParam(ctx, "month"))));

        app.exception(ServiceException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ServiceException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe");
        }
    }

    private static LocalDate dateParam(Context ctx, String name) {
        try {
            return LocalDate.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw ServiceException.invalid("Data inválida em " + name + "; use AAAA-MM-DD");
        }
    }

    private static YearMonth monthParam(Context ctx, String name) {
        try {
            return YearMonth.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw ServiceException.invalid("Mês inválido em " + name + "; use AAAA-MM");
        }
    }

    private static String required(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            throw ServiceException.invalid("Falta o parâmetro " + name);
        }
        return value;
    }
}
