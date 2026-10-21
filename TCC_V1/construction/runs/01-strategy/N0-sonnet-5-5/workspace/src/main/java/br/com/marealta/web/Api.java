package br.com.marealta.web;

import br.com.marealta.domain.Reservation;
import br.com.marealta.plan.RatePlans;
import br.com.marealta.service.ApiException;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.ReservationService.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Api {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Api() {}

    public static Javalin create(ReservationService service) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno"));

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("number", r.number());
            m.put("type", r.type());
            m.put("capacity", r.capacity());
            m.put("nightlyRate", r.nightlyRate());
            return m;
        }).toList()));

        app.post("/reservations", ctx -> json(ctx, 201, view(service.create(parseNew(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, document(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, document(service.paidReservation(id(ctx)))));

        app.get("/messages", ctx -> json(ctx, 200, service.messages()));

        app.get("/reports/occupancy", ctx -> csv(ctx, service.occupancyCsv(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            try {
                if (m == null || !m.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("", "", 0);
                csv(ctx, service.revenueCsv(YearMonth.parse(m)));
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("month deve estar no formato AAAA-MM");
            }
        });
        return app;
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }

    private static LocalDate date(String s) {
        if (s == null) throw ApiException.invalid("date é obrigatório");
        return parseDate(s, "date");
    }

    private static LocalDate parseDate(String s, String field) {
        try {
            if (!s.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("", s, 0);
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static NewReservation parseNew(String body) {
        JsonNode n;
        try {
            n = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (n == null || !n.isObject()) throw ApiException.invalid("Corpo deve ser um objeto JSON");

        String planCode = text(n, "ratePlan");
        var plan = RatePlans.find(planCode).orElseThrow(() -> ApiException.invalid("ratePlan desconhecido"));
        JsonNode guests = n.get("guests");
        JsonNode room = n.get("roomNumber");
        if (guests == null || !guests.isInt()) throw ApiException.invalid("guests deve ser um inteiro");
        if (room == null || !room.isInt()) throw ApiException.invalid("roomNumber deve ser um inteiro");
        JsonNode company = n.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;

        return new NewReservation(room.asInt(), text(n, "guestName"), text(n, "guestEmail"), text(n, "guestCpf"),
                parseDate(text(n, "checkIn"), "checkIn"), parseDate(text(n, "checkOut"), "checkOut"),
                guests.asInt(), plan, companyCode);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank()) {
            throw ApiException.invalid(field + " é obrigatório");
        }
        return v.asText();
    }

    private static Map<String, Object> view(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.room().number());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.put("guestCpf", r.guestCpf());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan().code());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    private static Map<String, Object> document(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", "Comprovante de reserva");
        m.put("reservationId", r.id());
        m.put("guestName", r.guestName());
        m.put("guestCpf", r.guestCpf());
        m.put("roomNumber", r.room().number());
        m.put("roomType", r.room().type());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        return m;
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
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
