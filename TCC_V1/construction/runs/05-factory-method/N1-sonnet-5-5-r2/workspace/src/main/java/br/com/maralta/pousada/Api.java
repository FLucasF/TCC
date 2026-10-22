package br.com.maralta.pousada;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Api {
    private Api() {
    }

    public static Javalin create(ReservationService service, Reports reports) {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(Json.MAPPER, false)));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, new ApiException(500, "INTERNAL_ERROR", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(Rooms.ALL.stream().map(Api::roomJson).toList()));

        app.post("/reservations", ctx -> {
            Reservation created = service.create(ReservationRequest.parse(ctx.body()));
            ctx.status(201).json(reservationJson(created));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(reservationJson(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> ctx.json(reservationJson(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> ctx.json(reservationJson(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> ctx.json(reservationJson(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> ctx.json(service.checkOut(ctx.pathParam("id")).toJson()));
        app.get("/reservations/{id}/document",
                ctx -> ctx.json(service.secondCopy(ctx.pathParam("id")).toJson()));

        app.get("/messages", ctx -> ctx.json(service.messages().stream().map(Message::toJson).toList()));

        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(dateParam(ctx, "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(monthParam(ctx, "month"))));
        return app;
    }

    private static void csv(Context ctx, String body) {
        ctx.contentType("text/csv; charset=utf-8").result(body);
    }

    private static LocalDate dateParam(Context ctx, String name) {
        try {
            return LocalDate.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + name + " (use 2027-07-10)");
        }
    }

    private static YearMonth monthParam(Context ctx, String name) {
        try {
            return YearMonth.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Mês inválido em " + name + " (use 2027-07)");
        }
    }

    private static String required(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            throw ApiException.invalidRequest("Parâmetro obrigatório: " + name);
        }
        return value;
    }

    private static void error(Context ctx, ApiException e) {
        ctx.status(e.httpStatus()).json(Map.of("error", e.code(), "message", e.getMessage()));
    }

    private static Map<String, Object> roomJson(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.id());
        json.put("roomNumber", r.roomNumber());
        json.put("guestName", r.guestName());
        json.put("guestEmail", r.guestEmail());
        json.putAll(Identifications.allFields(r.identification()));
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
