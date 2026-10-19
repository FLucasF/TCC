package br.com.mare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        createApp(new Hotel()).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }

    public static Javalin createApp(Hotel hotel) {
        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> json(ctx, 200, hotel.rooms().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("number", r.number());
            m.put("type", r.type());
            m.put("capacity", r.capacity());
            m.put("nightlyRate", r.nightlyRate());
            return m;
        }).toList()));

        app.post("/reservations", ctx -> json(ctx, 201, view(create(hotel, ctx))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(hotel.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(hotel.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(hotel.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(hotel.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, document(hotel.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, document(hotel.paid(ctx.pathParam("id")))));

        app.get("/messages", ctx -> json(ctx, 200, hotel.messages().stream().map(m -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", m.id());
            v.put("channel", m.channel());
            v.put("to", m.to());
            v.put("subject", m.subject());
            v.put("body", m.body());
            return v;
        }).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate date;
            try {
                date = LocalDate.parse(String.valueOf(ctx.queryParam("date")));
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro date inválido (use AAAA-MM-DD)");
            }
            ctx.contentType("text/csv; charset=utf-8").result(hotel.occupancyCsv(date));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month;
            try {
                month = YearMonth.parse(String.valueOf(ctx.queryParam("month")));
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro month inválido (use AAAA-MM)");
            }
            ctx.contentType("text/csv; charset=utf-8").result(hotel.revenueCsv(month));
        });

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno"));
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
        });
        return app;
    }

    // ---- criação ----

    private static Reservation create(Hotel hotel, Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.bodyAsBytes());
        } catch (Exception e) {
            throw ApiException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("Corpo da requisição deve ser um objeto JSON");

        JsonNode room = body.get("roomNumber");
        if (room == null || !room.isInt()) throw ApiException.invalid("roomNumber é obrigatório");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate in = date(body, "checkIn");
        LocalDate out = date(body, "checkOut");
        JsonNode g = body.get("guests");
        if (g == null || !g.isInt()) throw ApiException.invalid("guests é obrigatório");
        String planCode = text(body, "ratePlan");
        RatePlan plan = RatePlans.find(planCode);
        if (plan == null) throw ApiException.invalid("ratePlan desconhecido");
        if (!out.isAfter(in)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (g.asInt() < 1) throw ApiException.invalid("É preciso ao menos um hóspede");

        JsonNode cc = body.get("companyCode");
        String companyCode = cc != null && cc.isTextual() ? cc.asText() : null;
        return hotel.create(room.asInt(), name, email, cpf, in, out, g.asInt(), plan, companyCode);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) throw ApiException.invalid(field + " é obrigatório");
        return n.asText();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " inválida (use AAAA-MM-DD)");
        }
    }

    // ---- visões JSON ----

    private static Map<String, Object> view(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id);
        m.put("roomNumber", r.room.number());
        m.put("guestName", r.guestName);
        m.put("guestEmail", r.guestEmail);
        m.put("guestCpf", r.guestCpf);
        m.put("checkIn", r.checkIn.toString());
        m.put("checkOut", r.checkOut.toString());
        m.put("guests", r.guests);
        m.put("ratePlan", r.plan.code());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal);
        m.put("discount", r.discount);
        m.put("total", r.total);
        m.put("status", r.status);
        m.put("amountPaid", r.amountPaid);
        m.put("refund", r.refund);
        return m;
    }

    private static Map<String, Object> document(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", "Comprovante de reserva");
        m.put("reservationId", r.id);
        m.put("guestName", r.guestName);
        m.put("guestCpf", r.guestCpf);
        m.put("roomNumber", r.room.number());
        m.put("roomType", r.room.type());
        m.put("checkIn", r.checkIn.toString());
        m.put("checkOut", r.checkOut.toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal);
        m.put("discount", r.discount);
        m.put("total", r.total);
        return m;
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        json(ctx, status, m);
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
