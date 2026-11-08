package pousada.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.report.Reports;
import pousada.service.ReservationService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Api {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final Reports reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public Api(ReservationService service, Reports reports) {
        this.service = service;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(mapper, false));
        });

        app.get("/rooms", ctx -> ctx.json(RoomCatalog.all().stream().map(Api::roomView).toList()));
        app.post("/reservations", ctx -> ctx.status(201)
                .json(ReservationView.of(service.create(ReservationRequest.parse(mapper, ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(service.messages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(reports.occupancy(queryDate(ctx))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(reports.revenue(queryMonth(ctx))));

        app.exception(AppException.class, (e, ctx) -> error(ctx, e.code().status(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static Map<String, Object> roomView(Room room) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("number", room.number());
        view.put("type", room.type());
        view.put("capacity", room.type().capacity());
        view.put("nightlyRate", room.type().nightlyRate());
        return view;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente");
        }
    }

    private static LocalDate queryDate(Context ctx) {
        try {
            return LocalDate.parse(String.valueOf(ctx.queryParam("date")));
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Parâmetro date inválido (use AAAA-MM-DD)");
        }
    }

    private static YearMonth queryMonth(Context ctx) {
        try {
            return YearMonth.parse(String.valueOf(ctx.queryParam("month")));
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Parâmetro month inválido (use AAAA-MM)");
        }
    }
}
