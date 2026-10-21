package com.pousadamaretalta;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class App {
    private App() {}

    public static Javalin create(ReservationService service) {
        Javalin app = Javalin.create();

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno"));

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(App::roomJson).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(service.create(Json.parseObject(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, reservationJson(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, reservationJson(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, reservationJson(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, documentJson(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, documentJson(service.document(ctx.pathParam("id")))));
        app.get("/messages", ctx -> json(ctx, 200, service.messages()));
        app.get("/reports/occupancy",
                ctx -> csv(ctx, service.occupancyCsv(ctx.queryParam("date"))));
        app.get("/reports/revenue",
                ctx -> csv(ctx, service.revenueCsv(ctx.queryParam("month"))));
        return app;
    }

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json; charset=utf-8").result(Json.MAPPER.writeValueAsString(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (Exception ignored) {
        }
    }

    private static Map<String, Object> roomJson(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
        return m;
    }

    private static void identityFields(Map<String, Object> m, GuestIdentity i) {
        m.put("guestCpf", i.cpf());
        m.put("companyCnpj", i.cnpj());
        m.put("companyName", i.companyName());
        m.put("guestPassport", i.passport());
        m.put("guestCountry", i.country());
    }

    private static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id);
        m.put("roomNumber", r.room.number());
        m.put("guestName", r.guestName);
        m.put("guestEmail", r.guestEmail);
        identityFields(m, r.identity);
        m.put("checkIn", r.checkIn.toString());
        m.put("checkOut", r.checkOut.toString());
        m.put("guests", r.guests);
        m.put("ratePlan", r.ratePlan);
        m.put("nights", r.quote.nights());
        m.put("subtotal", r.quote.subtotal());
        m.put("discount", r.quote.discount());
        m.put("total", r.quote.total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    private static Map<String, Object> documentJson(ReservationDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", d.title());
        m.put("reservationId", d.reservationId());
        m.put("guestName", d.guestName());
        identityFields(m, d.identity());
        m.put("roomNumber", d.roomNumber());
        m.put("roomType", d.roomType());
        m.put("checkIn", d.checkIn().toString());
        m.put("checkOut", d.checkOut().toString());
        m.put("nights", d.nights());
        m.put("subtotal", d.subtotal());
        m.put("discount", d.discount());
        m.put("total", d.total());
        m.put("text", d.text());
        return m;
    }
}
