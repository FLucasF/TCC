package br.com.mealta.web;

import br.com.mealta.domain.ErrorCode;
import br.com.mealta.domain.PousadaException;
import br.com.mealta.domain.ReservationService;
import br.com.mealta.domain.RoomCatalog;
import br.com.mealta.report.CsvRenderer;
import br.com.mealta.report.ReportCatalog;
import br.com.mealta.report.ReportDefinition;
import br.com.mealta.store.MessageLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

public class Api {
    private static final Logger log = LoggerFactory.getLogger(Api.class);

    private final ReservationService reservations;
    private final RoomCatalog rooms;
    private final MessageLog messages;
    private final ReportCatalog reports;
    private final CsvRenderer csv = new CsvRenderer();
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final CreateReservationParser parser = new CreateReservationParser(mapper);

    public Api(ReservationService reservations, RoomCatalog rooms, MessageLog messages, ReportCatalog reports) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.messages = messages;
        this.reports = reports;
    }

    public Javalin build() {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(Views::of).toList()));
        app.post("/reservations", ctx -> ctx.status(201)
                .json(Views.of(reservations.create(parser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(Views.of(reservations.get(id(ctx)))));
        reservationAction(app, "confirmation", reservations::confirm);
        reservationAction(app, "cancellation", reservations::cancel);
        reservationAction(app, "check-in", reservations::checkIn);
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(Views.of(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(Views.of(reservations.document(id(ctx)))));
        app.get("/messages", ctx -> ctx.json(messages.all().stream().map(Views::of).toList()));

        for (ReportDefinition report : reports.definitions()) {
            app.get("/reports/" + report.name(), ctx -> ctx
                    .contentType("text/csv; charset=utf-8")
                    .result(csv.render(report.builder().apply(ctx.queryParam(report.param())))));
        }

        app.exception(PousadaException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Erro inesperado", e);
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private void reservationAction(Javalin app, String action,
                                   Function<Long, br.com.mealta.domain.Reservation> operation) {
        app.post("/reservations/{id}/" + action, ctx -> ctx.json(Views.of(operation.apply(id(ctx)))));
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + raw + " não existe");
        }
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        ctx.status(code.httpStatus()).json(Map.of("error", code.name(), "message", message));
    }
}
