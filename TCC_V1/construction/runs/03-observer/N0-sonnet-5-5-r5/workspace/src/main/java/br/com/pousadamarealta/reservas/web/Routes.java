package br.com.pousadamarealta.reservas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.JsonGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;
import br.com.pousadamarealta.reservas.domain.PousadaException;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.MessageRepository;
import br.com.pousadamarealta.reservas.service.ReportService;
import br.com.pousadamarealta.reservas.service.ReservationService;

import static br.com.pousadamarealta.reservas.domain.PousadaException.Code.RESERVATION_NOT_FOUND;

public class Routes {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();

    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageRepository messages;
    private final Database db;

    public Routes(ReservationService reservations, ReportService reports, MessageRepository messages, Database db) {
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
        this.db = db;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> json(ctx, 200, reservations.listRooms()));
        app.post("/reservations", ctx ->
                json(ctx, 201, reservations.create(RequestParser.createReservation(MAPPER, ctx.bodyAsBytes()))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, db.transaction(messages::findAll)));
        app.get("/reports/occupancy", ctx ->
                csv(ctx, reports.occupancy(RequestParser.queryDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx ->
                csv(ctx, reports.revenue(RequestParser.queryMonth(ctx.queryParam("month")))));

        app.exception(PousadaException.class, (e, ctx) ->
                error(ctx, e.code().httpStatus, e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            org.slf4j.LoggerFactory.getLogger(Routes.class).error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(RESERVATION_NOT_FOUND, "Reserva " + ctx.pathParam("id") + " não existe");
        }
    }

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("error", code).put("message", message);
        ctx.status(status).contentType("application/json").result(node.toString());
    }
}
