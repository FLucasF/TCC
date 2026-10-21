package pousada.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.util.Map;
import pousada.error.BusinessException;
import pousada.error.ErrorCode;
import pousada.report.Report;
import pousada.report.ReportCatalog;
import pousada.report.ReportWriter;
import pousada.service.Notifications;
import pousada.service.Outbox;
import pousada.service.ReservationRepository;
import pousada.service.ReservationService;
import pousada.service.RoomCatalog;

public final class Api {
    private Api() {
    }

    public static Javalin create() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RoomCatalog rooms = new RoomCatalog();
        ReservationRepository repository = new ReservationRepository();
        Outbox outbox = new Outbox();
        ReservationService service = new ReservationService(rooms, repository, new Notifications(outbox));
        ReportWriter writer = new ReportWriter();

        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.exception(BusinessException.class, (e, ctx) -> ctx.status(e.code().httpStatus())
                .json(Map.of("error", e.code().name(), "message", e.getMessage())));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(Views::of).toList()));
        app.get("/messages", ctx -> ctx.json(outbox.sent()));

        app.post("/reservations", ctx -> ctx.status(201)
                .json(Views.of(service.create(ReservationRequest.parse(mapper, ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(Views.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(Views.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(Views.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(Views.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.receipt(id(ctx))));

        for (Report report : ReportCatalog.all(repository, rooms)) {
            app.get("/reports/" + report.name(), ctx -> {
                String period = ctx.queryParam(report.parameter());
                if (period == null) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST,
                            "Informe o parâmetro " + report.parameter());
                }
                ctx.contentType("text/csv; charset=utf-8").result(writer.write(report, report.generate(period)));
            });
        }
        return app;
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente");
        }
    }
}
