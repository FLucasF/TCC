package br.com.marealta;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("A variável de ambiente DB_URL é obrigatória.");
        }
        Db db = new Db(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        db.init();
        String port = System.getenv("PORT");
        int p = port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
        create(new ReservationService(db)).start(p);
    }

    public static Javalin create(ReservationService svc) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno.");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado.");
            }
        });

        app.get("/rooms", ctx -> json(ctx, 200, svc.rooms().stream().map(App::roomJson).toList()));

        app.post("/reservations", ctx -> {
            var cmd = RequestParser.parseNewReservation(ctx.body());
            json(ctx, 201, reservationJson(svc.create(cmd)));
        });
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(svc.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationJson(svc.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationJson(svc.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationJson(svc.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> {
            Reservation r = svc.checkOut(id(ctx));
            json(ctx, 200, documentJson(r, svc.roomOf(r)));
        });
        app.get("/reservations/{id}/document", ctx -> {
            Reservation r = svc.get(id(ctx));
            if (!r.status().isPaid()) {
                throw ApiException.notAllowed("Só há comprovante para reserva paga.");
            }
            json(ctx, 200, documentJson(r, svc.roomOf(r)));
        });

        app.get("/messages", ctx -> json(ctx, 200, svc.messages().stream().map(App::messageJson).toList()));

        app.get("/reports/occupancy", ctx ->
                csv(ctx, svc.occupancyCsv(RequestParser.parseDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx ->
                csv(ctx, svc.revenueCsv(RequestParser.parseMonth(ctx.queryParam("month")))));
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não existe.");
        }
    }

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        try {
            json(ctx, status, m);
        } catch (Exception e) {
            ctx.status(status).result(code);
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

    private static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.roomNumber());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.put("guestCpf", r.guestCpf());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    private static Map<String, Object> documentJson(Reservation r, Room room) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", Receipt.TITLE);
        m.put("reservationId", r.id());
        m.put("guestName", r.guestName());
        m.put("guestCpf", r.guestCpf());
        m.put("roomNumber", room.number());
        m.put("roomType", room.type());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        return m;
    }

    private static Map<String, Object> messageJson(Message m) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", m.id());
        o.put("channel", m.channel());
        o.put("to", m.to());
        o.put("subject", m.subject());
        o.put("body", m.body());
        return o;
    }
}
