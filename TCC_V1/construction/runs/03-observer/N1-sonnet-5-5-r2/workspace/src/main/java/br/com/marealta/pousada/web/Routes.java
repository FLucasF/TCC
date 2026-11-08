package br.com.marealta.pousada.web;

import br.com.marealta.pousada.app.ReportService;
import br.com.marealta.pousada.app.ReservationService;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.notification.Message;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.MessageRepository;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Routes {

    private static final String JSON = "application/json; charset=utf-8";
    private static final String CSV = "text/csv; charset=utf-8";

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
        app.exception(PousadaException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));

        app.get("/rooms", ctx -> json(ctx, 200, Room.ALL.stream().map(Routes::room).toList()));
        app.post("/reservations", ctx ->
                json(ctx, 201, Json.reservation(reservations.create(BookingParser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Json.reservation(reservations.find(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx ->
                json(ctx, 200, Json.reservation(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx ->
                json(ctx, 200, Json.reservation(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx ->
                json(ctx, 200, Json.reservation(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, db.transaction(messages::findAll).stream()
                .map(Routes::message).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "Informe date no formato 2027-07-10");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "Informe month no formato 2027-07");
        }
    }

    private static Map<String, Object> room(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    private static Map<String, Object> message(Message m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", m.id());
        json.put("channel", m.channel());
        json.put("to", m.to());
        json.put("subject", m.subject());
        json.put("body", m.body());
        return json;
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        json(ctx, code.httpStatus(), Map.of("error", code.name(), "message", message));
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType(JSON).result(Json.MAPPER.writeValueAsString(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType(CSV).result(body);
    }
}
