package br.com.marealta.web;

import br.com.marealta.ApiException;
import br.com.marealta.model.*;
import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;
import br.com.marealta.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private final ReservationService reservations;
    private final ReportService reports;

    public App(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, reservations.rooms().stream().map(App::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, view(reservations.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, receiptView(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, receiptView(reservations.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, reservations.messages().stream().map(App::messageView).toList()));

        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));

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

    // ---- entrada ----

    private static ReservationService.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.bodyAsBytes());
        } catch (java.io.IOException e) {
            throw ApiException.invalid("O corpo da requisição não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("O corpo da requisição deve ser um objeto JSON");

        JsonNode room = body.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("roomNumber é obrigatório e deve ser um número inteiro");
        }
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate in = parseDate(text(body, "checkIn"), "checkIn");
        LocalDate out = parseDate(text(body, "checkOut"), "checkOut");
        JsonNode g = body.get("guests");
        if (g == null || !g.isIntegralNumber() || !g.canConvertToInt()) {
            throw ApiException.invalid("guests é obrigatório e deve ser um número inteiro");
        }
        String planCode = text(body, "ratePlan");
        RatePlan plan = RatePlans.find(planCode)
                .orElseThrow(() -> ApiException.invalid("Plano de tarifa desconhecido: " + planCode));
        if (!out.isAfter(in)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (g.intValue() < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");

        JsonNode company = body.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.textValue() : null;
        return new ReservationService.NewReservation(room.intValue(), name, email, cpf, in, out, g.intValue(), plan, companyCode);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.textValue().isBlank()) {
            throw ApiException.invalid(field + " é obrigatório");
        }
        return n.textValue();
    }

    private static LocalDate parseDate(String s, String field) {
        if (s == null) throw ApiException.invalid(field + " é obrigatório (formato AAAA-MM-DD)");
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " inválido, use o formato AAAA-MM-DD");
        }
    }

    private static YearMonth parseMonth(String s) {
        if (s == null) throw ApiException.invalid("month é obrigatório (formato AAAA-MM)");
        try {
            return YearMonth.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month inválido, use o formato AAAA-MM");
        }
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe");
        }
    }

    // ---- saída ----

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json; charset=utf-8")
                    .result(JSON.writeValueAsBytes(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body.getBytes(StandardCharsets.UTF_8));
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        json(ctx, status, m);
    }

    private static Map<String, Object> roomView(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
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
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("status", r.status().name());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    private static Map<String, Object> receiptView(Receipt d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", Receipt.TITLE);
        m.put("reservationId", d.reservationId());
        m.put("guestName", d.guestName());
        m.put("guestCpf", d.guestCpf());
        m.put("roomNumber", d.roomNumber());
        m.put("roomType", d.roomType());
        m.put("checkIn", d.checkIn().toString());
        m.put("checkOut", d.checkOut().toString());
        m.put("nights", d.nights());
        m.put("subtotal", d.subtotal());
        m.put("discount", d.discount());
        m.put("total", d.total());
        return m;
    }

    private static Map<String, Object> messageView(Message m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", m.id());
        out.put("channel", m.channel());
        out.put("to", m.to());
        out.put("subject", m.subject());
        out.put("body", m.body());
        return out;
    }
}
