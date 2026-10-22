package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        Database db = new Database(System.getenv("DB_URL"), System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
        db.initialize();
        int port = System.getenv("PORT") == null ? 7070 : Integer.parseInt(System.getenv("PORT"));
        routes(Javalin.create(), db).start(port);
    }

    static Javalin routes(Javalin app, Database db) {
        Repository repository = new Repository();
        ReservationService service = new ReservationService(db, repository);
        Reports reports = new Reports(db, repository);

        app.exception(ApiException.class, (e, ctx) -> ctx.status(e.status).json(error(e.code, e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(error("INTERNAL_ERROR", "Erro interno."));
        });

        app.get("/rooms", ctx -> ctx.json(service.rooms().stream().map(Main::roomView).toList()));
        app.post("/reservations", ctx -> ctx.status(201).json(view(service.create(body(ctx)))));
        app.get("/reservations/{id}", ctx -> ctx.json(view(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(view(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(view(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(view(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(documentView(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(documentView(service.paidReservation(id(ctx)))));
        app.get("/messages", ctx -> ctx.json(service.messages().stream().map(Main::messageView).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(ctx.queryParam("date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(ctx.queryParam("month"))));
        return app;
    }

    private static void csv(Context ctx, String content) {
        ctx.contentType("text/csv; charset=utf-8").result(content);
    }

    private static JsonNode body(Context ctx) {
        try {
            JsonNode node = MAPPER.readTree(ctx.bodyAsBytes());
            if (node != null && node.isObject()) {
                return node;
            }
        } catch (Exception ignored) {
            // cai no erro abaixo
        }
        throw ApiException.invalid("O corpo deve ser um objeto JSON.");
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe.");
        }
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error", code);
        map.put("message", message);
        return map;
    }

    private static Map<String, Object> roomView(Room r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("number", r.number());
        map.put("type", r.type());
        map.put("capacity", r.capacity());
        map.put("nightlyRate", r.nightlyRate());
        return map;
    }

    private static Map<String, Object> messageView(Message m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.id());
        map.put("channel", m.channel());
        map.put("to", m.to());
        map.put("subject", m.subject());
        map.put("body", m.body());
        return map;
    }

    private static void putIdentification(Map<String, Object> map, Reservation r) {
        for (String field : IdentificationTypes.allFields()) {
            map.put(field, r.identification().values().get(field));
        }
    }

    private static Map<String, Object> view(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.id());
        map.put("roomNumber", r.room().number());
        map.put("guestName", r.guestName());
        map.put("guestEmail", r.guestEmail());
        putIdentification(map, r);
        map.put("checkIn", r.checkIn().toString());
        map.put("checkOut", r.checkOut().toString());
        map.put("guests", r.guests());
        map.put("ratePlan", r.ratePlan().name());
        map.put("nights", r.pricing().nights());
        map.put("subtotal", r.pricing().subtotal());
        map.put("discount", r.pricing().discount());
        map.put("total", r.pricing().total());
        map.put("status", r.status().name());
        map.put("amountPaid", r.amountPaid());
        map.put("refund", r.refund());
        return map;
    }

    private static Map<String, Object> documentView(Reservation r) {
        Document document = r.identification().type().document();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", document.title());
        map.put("reservationId", r.id());
        map.put("guestName", r.guestName());
        putIdentification(map, r);
        map.put("roomNumber", r.room().number());
        map.put("roomType", r.room().type());
        map.put("checkIn", r.checkIn().toString());
        map.put("checkOut", r.checkOut().toString());
        map.put("nights", r.pricing().nights());
        map.put("subtotal", r.pricing().subtotal());
        map.put("discount", r.pricing().discount());
        map.put("total", r.pricing().total());
        map.put("text", document.render(r));
        return map;
    }
}
