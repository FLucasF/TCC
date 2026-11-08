package pousada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.plan.RatePlan;
import pousada.plan.RatePlans;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Api {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReservationService service;

    private Api(ReservationService service) {
        this.service = service;
    }

    public static Javalin create(ReservationService service) {
        Api api = new Api(service);
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.get("/rooms", api::rooms);
        app.post("/reservations", api::create);
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationJson(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationJson(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationJson(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, documentJson(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, documentJson(service.document(id(ctx)))));
        app.get("/messages", api::messages);
        app.get("/reports/occupancy", ctx -> csv(ctx, service.occupancyCsv(
                date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, service.revenueCsv(month(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    // ---------- handlers ----------

    private void rooms(Context ctx) {
        List<Map<String, Object>> out = service.rooms().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("number", r.number());
            m.put("type", r.type());
            m.put("capacity", r.capacity());
            m.put("nightlyRate", money(r.nightlyRate()));
            return m;
        }).toList();
        json(ctx, 200, out);
    }

    private void messages(Context ctx) {
        List<Map<String, Object>> out = service.messages().stream().map(m -> {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", m.id());
            o.put("channel", m.channel());
            o.put("to", m.to());
            o.put("subject", m.subject());
            o.put("body", m.body());
            return o;
        }).toList();
        json(ctx, 200, out);
    }

    private void create(Context ctx) {
        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo precisa ser um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("O corpo precisa ser um objeto JSON");

        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = date(text(body, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(body, "checkOut"), "checkOut");
        JsonNode roomNode = body.get("roomNumber");
        JsonNode guestsNode = body.get("guests");
        if (roomNode == null || !roomNode.isIntegralNumber() || !roomNode.canConvertToInt()) {
            throw ApiException.invalid("roomNumber é obrigatório e deve ser um número inteiro");
        }
        if (guestsNode == null || !guestsNode.isIntegralNumber() || !guestsNode.canConvertToInt()) {
            throw ApiException.invalid("guests é obrigatório e deve ser um número inteiro");
        }
        String planName = text(body, "ratePlan");
        RatePlan plan = RatePlans.find(planName)
                .orElseThrow(() -> ApiException.invalid("ratePlan desconhecido: " + planName));
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída precisa ser depois da entrada");
        int guests = guestsNode.intValue();
        if (guests < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");
        JsonNode company = body.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.textValue() : null;

        Reservation r = service.create(new ReservationService.NewReservation(
                roomNode.intValue(), name, email, cpf, checkIn, checkOut, guests, plan, companyCode));
        json(ctx, 201, reservationJson(r));
    }

    // ---------- conversões ----------

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
        m.put("subtotal", money(r.subtotal()));
        m.put("discount", money(r.discount()));
        m.put("total", money(r.total()));
        m.put("status", r.status());
        m.put("amountPaid", money(r.amountPaid()));
        m.put("refund", r.refund() == null ? null : money(r.refund()));
        return m;
    }

    private static Map<String, Object> documentJson(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", "Comprovante de reserva");
        m.put("reservationId", r.id());
        m.put("guestName", r.guestName());
        m.put("guestCpf", r.guestCpf());
        m.put("roomNumber", r.roomNumber());
        m.put("roomType", r.roomType());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", money(r.subtotal()));
        m.put("discount", money(r.discount()));
        m.put("total", money(r.total()));
        return m;
    }

    private static BigDecimal money(BigDecimal v) {
        return Money.round(v);
    }

    // ---------- utilitários ----------

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe");
        }
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.textValue().isBlank()) {
            throw ApiException.invalid(field + " é obrigatório");
        }
        return n.textValue();
    }

    private static LocalDate date(String raw, String field) {
        if (raw == null) throw ApiException.invalid(field + " é obrigatório (formato 2027-07-10)");
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato 2027-07-10");
        }
    }

    private static YearMonth month(String raw) {
        if (raw == null) throw ApiException.invalid("month é obrigatório (formato 2027-07)");
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month deve estar no formato 2027-07");
        }
    }

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
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        json(ctx, status, m);
    }
}
