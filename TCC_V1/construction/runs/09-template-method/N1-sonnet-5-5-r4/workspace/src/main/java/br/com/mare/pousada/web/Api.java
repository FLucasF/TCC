package br.com.mare.pousada.web;

import br.com.mare.pousada.domain.BusinessException;
import br.com.mare.pousada.domain.ErrorCode;
import br.com.mare.pousada.domain.RoomCatalog;
import br.com.mare.pousada.infra.Db;
import br.com.mare.pousada.infra.MessageRepository;
import br.com.mare.pousada.infra.ReservationRepository;
import br.com.mare.pousada.report.Report;
import br.com.mare.pousada.report.Reports;
import br.com.mare.pousada.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;
import java.util.function.Function;

public final class Api {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final Db db;
    private final ReservationService service;

    private Api(Db db) {
        ReservationRepository reservations = new ReservationRepository();
        MessageRepository messages = new MessageRepository();
        this.db = db;
        this.service = new ReservationService(db, reservations, messages);
    }

    public static Javalin create(Db db) {
        return new Api(db).routes();
    }

    private Javalin routes() {
        Javalin app = Javalin.create();
        app.exception(BusinessException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, RoomCatalog.all().stream().map(r -> Map.of(
                "number", r.number(), "type", r.type(), "capacity", r.capacity(),
                "nightlyRate", r.nightlyRate())).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                service.create(CreateReservationRequest.parse(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, service.sentMessages()));

        for (Report<?> report : Reports.all(new ReservationRepository())) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = db.inTransaction(c -> report.render(c, ctx.queryParam(report.parameter())));
                ctx.status(200).contentType("text/csv; charset=utf-8").result(csv);
            });
        }
        return app;
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + raw + " não existe");
        }
    }

    private void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
    }

    private void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (Exception e) {
            ctx.status(status).result(code);
        }
    }
}
