package br.com.marealta.web;

import br.com.marealta.*;
import br.com.marealta.plan.RatePlans;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rotas HTTP e conversão de/para JSON. */
public final class Api {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ReservationService reservations;
    private final ReportService reports;

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public void register(Javalin app) {
        app.get("/rooms", ctx -> json(ctx, 200, reservations.rooms().stream().map(Api::roomJson).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(reservations.create(parseCreate(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationJson(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationJson(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationJson(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, documentJson(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, documentJson(reservations.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, reservations.messages().stream().map(Api::messageJson).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(queryDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(queryMonth(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.error(404, ctx -> {
            if (ctx.attribute("handled") == null) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado.");
            }
        });
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno.");
        });
    }

    // ---- entrada
    private static ReservationService.CreateCommand parseCreate(String body) {
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido.");
        }
        if (root == null || !root.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON.");

        String guestName = text(root, "guestName");
        String guestEmail = text(root, "guestEmail");
        String guestCpf = text(root, "guestCpf");
        String ratePlan = text(root, "ratePlan");
        LocalDate checkIn = date(root, "checkIn");
        LocalDate checkOut = date(root, "checkOut");
        JsonNode room = root.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) throw ApiException.invalid("roomNumber é obrigatório e deve ser inteiro.");
        JsonNode guests = root.get("guests");
        if (guests == null || !guests.isIntegralNumber() || !guests.canConvertToInt() || guests.asInt() < 1) {
            throw ApiException.invalid("guests é obrigatório e deve ser um inteiro maior que zero.");
        }
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("checkOut deve ser depois de checkIn.");
        if (RatePlans.find(ratePlan).isEmpty()) throw ApiException.invalid("ratePlan desconhecido: " + ratePlan);

        JsonNode company = root.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;
        return new ReservationService.CreateCommand(room.asInt(), guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests.asInt(), ratePlan, companyCode);
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) throw ApiException.invalid(field + " é obrigatório.");
        return n.asText();
    }

    private static LocalDate date(JsonNode root, String field) {
        return queryDate(text(root, field), field);
    }

    private static LocalDate queryDate(String value) {
        return queryDate(value, "date");
    }

    private static LocalDate queryDate(String value, String field) {
        if (value == null) throw ApiException.invalid(field + " é obrigatório.");
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato AAAA-MM-DD.");
        }
    }

    private static YearMonth queryMonth(String value) {
        if (value == null) throw ApiException.invalid("month é obrigatório.");
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month deve estar no formato AAAA-MM.");
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound("Reserva " + ctx.pathParam("id") + " não existe.");
        }
    }

    // ---- saída
    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsBytes(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body.getBytes(StandardCharsets.UTF_8));
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.attribute("handled", true);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        json(ctx, status, body);
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

    private static Map<String, Object> documentJson(ReservationService.Booking b) {
        Reservation r = b.reservation();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", Receipt.TITLE);
        m.put("reservationId", r.id());
        m.put("guestName", r.guestName());
        m.put("guestCpf", r.guestCpf());
        m.put("roomNumber", b.room().number());
        m.put("roomType", b.room().type());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        return m;
    }

    private static Map<String, Object> messageJson(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        return m;
    }
}
