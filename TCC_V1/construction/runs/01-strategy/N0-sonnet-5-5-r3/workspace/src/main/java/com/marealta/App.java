package com.marealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class App {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ReservationService service;

    public App(ReservationService service) {
        this.service = service;
    }

    public Javalin create() {
        Javalin app = Javalin.create();
        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(App::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, view(service.create(parseNew(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, service.messages().stream().map(App::messageView).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, service.occupancyReport(queryDate(ctx, "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, service.revenueReport(queryMonth(ctx, "month"))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
        });
        return app;
    }

    // ---- respostas

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        try {
            json(ctx, status, body);
        } catch (Exception e) {
            ctx.status(status);
        }
    }

    private static Map<String, Object> roomView(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
        return m;
    }

    private static Map<String, Object> messageView(Message x) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", x.id());
        m.put("channel", x.channel());
        m.put("to", x.to());
        m.put("subject", x.subject());
        m.put("body", x.body());
        return m;
    }

    private static Map<String, Object> view(Reservation r) {
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
        m.put("companyCode", r.companyCode());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    // ---- entrada

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + raw);
        }
    }

    private static LocalDate queryDate(Context ctx, String name) {
        String v = ctx.queryParam(name);
        if (v == null) throw ApiException.invalid("Parâmetro obrigatório: " + name);
        return date(v, name);
    }

    private static YearMonth queryMonth(Context ctx, String name) {
        String v = ctx.queryParam(name);
        if (v == null) throw ApiException.invalid("Parâmetro obrigatório: " + name);
        if (!v.matches("\\d{4}-\\d{2}")) throw ApiException.invalid("Mês inválido (use AAAA-MM): " + v);
        try {
            return YearMonth.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Mês inválido (use AAAA-MM): " + v);
        }
    }

    private static LocalDate date(String v, String field) {
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida em " + field + " (use AAAA-MM-DD): " + v);
        }
    }

    static ReservationService.NewReservation parseNew(String body) {
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalid("O corpo deve ser um JSON válido");
        }
        if (root == null || !root.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");

        int roomNumber = integer(root, "roomNumber");
        String name = text(root, "guestName");
        String email = text(root, "guestEmail");
        String cpf = text(root, "guestCpf");
        LocalDate in = date(text(root, "checkIn"), "checkIn");
        LocalDate out = date(text(root, "checkOut"), "checkOut");
        int guests = integer(root, "guests");
        String plan = text(root, "ratePlan");
        JsonNode cc = root.get("companyCode");
        String company = cc != null && cc.isTextual() ? cc.asText() : null;

        if (!out.isAfter(in)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");
        return new ReservationService.NewReservation(roomNumber, name.trim(), email.trim(), cpf.trim(), in, out,
                guests, plan.trim(), company);
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText();
    }

    private static int integer(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asInt();
    }
}
