package br.com.marealta.web;

import br.com.marealta.ReservationService;
import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.mail.Message;
import br.com.marealta.persistence.Db;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.RoomRepository;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public final class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();
    private final RequestParser parser = new RequestParser(mapper);

    private final Db db;
    private final RoomRepository rooms;
    private final MessageRepository messages;
    private final ReservationService reservations;
    private final OccupancyReport occupancy;
    private final RevenueReport revenue;

    public Routes(Db db, RoomRepository rooms, MessageRepository messages, ReservationService reservations,
                  OccupancyReport occupancy, RevenueReport revenue) {
        this.db = db;
        this.rooms = rooms;
        this.messages = messages;
        this.reservations = reservations;
        this.occupancy = occupancy;
        this.revenue = revenue;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> json(ctx, 200, db.transaction(rooms::findAll)));
        app.post("/reservations", ctx -> json(ctx, 201,
                ReservationResponse.of(reservations.create(parser.reservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> reservation(ctx, reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> reservation(ctx, reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> reservation(ctx, reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> reservation(ctx, reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> {
            List<Message> sent = db.transaction(messages::findAll);
            json(ctx, 200, sent);
        });
        app.get("/reports/occupancy", ctx ->
                csv(ctx, occupancy.generate(parser.day(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx ->
                csv(ctx, revenue.generate(parser.month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) -> json(ctx, e.code().httpStatus(), error(e.code().name(), e)));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            json(ctx, 500, Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
    }

    private long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada");
        }
    }

    private void reservation(Context ctx, br.com.marealta.domain.Reservation reservation) {
        json(ctx, 200, ReservationResponse.of(reservation));
    }

    private Map<String, String> error(String code, Exception e) {
        return Map.of("error", code, "message", String.valueOf(e.getMessage()));
    }

    private void csv(Context ctx, String content) {
        ctx.status(200).contentType(CSV).result(content);
    }

    private void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
