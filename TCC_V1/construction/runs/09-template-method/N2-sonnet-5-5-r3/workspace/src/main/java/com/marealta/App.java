package com.marealta;

import com.marealta.report.ReportCatalog;
import com.marealta.report.ReportData;
import io.javalin.Javalin;
import io.javalin.http.Context;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class App {
    private static final String CSV = "text/csv; charset=utf-8";

    private final RoomCatalog rooms = new RoomCatalog();
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final ReservationService service;
    private final ReportCatalog reports = new ReportCatalog();

    public App(DataSource dataSource) {
        Database.createSchema(dataSource);
        this.reservations = new ReservationRepository(dataSource);
        this.messages = new MessageRepository(dataSource);
        this.service = new ReservationService(rooms, reservations, messages);
    }

    public Javalin create() {
        Javalin app = Javalin.create();

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(App::roomJson).toList()));

        app.post("/reservations", ctx -> {
            Reservation created = service.create(ReservationRequest.parse(ctx.body()));
            ctx.status(201).json(reservationJson(created));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(reservationJson(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservationJson(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservationJson(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservationJson(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(receiptJson(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(receiptJson(service.document(ctx.pathParam("id")))));

        app.get("/messages", ctx -> ctx.json(messages.all()));

        ReportData data = new ReportData(reservations, rooms);
        for (ReportCatalog.Definition report : reports.all()) {
            app.get("/reports/" + report.name(), ctx ->
                    ctx.contentType(CSV).result(report.factory().apply(data, ctx.queryParam(report.parameter())).render()));
        }
        return app;
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static Map<String, Object> roomJson(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    private static Map<String, Object> receiptJson(Receipt r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", r.title());
        json.put("reservationId", r.reservationId());
        json.put("guestName", r.guestName());
        json.put("guestCpf", r.guestCpf());
        json.put("roomNumber", r.roomNumber());
        json.put("roomType", r.roomType());
        json.put("checkIn", r.checkIn().toString());
        json.put("checkOut", r.checkOut().toString());
        json.put("nights", r.nights());
        json.put("subtotal", r.subtotal());
        json.put("discount", r.discount());
        json.put("total", r.total());
        return json;
    }

    private static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.id());
        json.put("roomNumber", r.roomNumber());
        json.put("guestName", r.guestName());
        json.put("guestEmail", r.guestEmail());
        json.put("guestCpf", r.guestCpf());
        json.put("checkIn", r.checkIn().toString());
        json.put("checkOut", r.checkOut().toString());
        json.put("guests", r.guests());
        json.put("ratePlan", r.ratePlan());
        json.put("nights", r.nights());
        json.put("subtotal", r.subtotal());
        json.put("discount", r.discount());
        json.put("total", r.total());
        json.put("status", r.status());
        json.put("amountPaid", r.amountPaid());
        json.put("refund", r.refund());
        return json;
    }
}
