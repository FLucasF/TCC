package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

public class WebApp {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final ReservationService service;

    public WebApp(ReservationService service) { this.service = service; }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(WebApp::roomJson).toList()));

        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(service.create(parseNew(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationJson(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationJson(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationJson(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, receiptJson(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, receiptJson(service.document(id(ctx)))));

        app.get("/messages", ctx -> json(ctx, 200, service.messages().stream().map(WebApp::messageJson).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"), "date");
            csv(ctx, service.occupancyCsv(date));
        });
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            YearMonth month;
            try {
                if (m == null || !m.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("", "", 0);
                month = YearMonth.parse(m);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("O parâmetro month deve estar no formato AAAA-MM.");
            }
            csv(ctx, service.revenueCsv(month));
        });

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno.");
        });
        return app;
    }

    // ---------- entrada ----------

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + ctx.pathParam("id") + " não existe.");
        }
    }

    private static LocalDate parseDate(String s, String field) {
        try {
            if (s == null || !s.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("", "", 0);
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("O campo " + field + " deve ser uma data no formato AAAA-MM-DD.");
        }
    }

    static NewReservation parseNew(String body) {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo da requisição não é um JSON válido.");
        }
        if (root == null || !root.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON.");

        JsonNode room = root.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("roomNumber é obrigatório e deve ser um número inteiro.");
        }
        JsonNode guests = root.get("guests");
        if (guests == null || !guests.isIntegralNumber() || !guests.canConvertToInt()) {
            throw ApiException.invalid("guests é obrigatório e deve ser um número inteiro.");
        }
        String name = text(root, "guestName");
        String email = text(root, "guestEmail");
        String cpf = text(root, "guestCpf");
        LocalDate in = parseDate(text(root, "checkIn"), "checkIn");
        LocalDate out = parseDate(text(root, "checkOut"), "checkOut");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(root, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE.");
        }
        if (!out.isAfter(in)) throw ApiException.invalid("A saída deve ser depois da entrada.");
        if (guests.asInt() < 1) throw ApiException.invalid("É preciso pelo menos um hóspede.");
        return new NewReservation(room.asInt(), name, email, cpf, in, out, guests.asInt(), plan);
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid(field + " é obrigatório e deve ser um texto.");
        }
        return n.asText().trim();
    }

    // ---------- saída ----------

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        json(ctx, status, Map.of("error", code, "message", message));
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
        m.put("ratePlan", r.ratePlan().name());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("status", r.status().name());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    private static Map<String, Object> receiptJson(Receipt r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", Receipt.TITLE);
        m.put("reservationId", r.reservationId());
        m.put("guestName", r.guestName());
        m.put("guestCpf", r.guestCpf());
        m.put("roomNumber", r.roomNumber());
        m.put("roomType", r.roomType());
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
        o.put("status", m.status().name());
        return o;
    }
}
