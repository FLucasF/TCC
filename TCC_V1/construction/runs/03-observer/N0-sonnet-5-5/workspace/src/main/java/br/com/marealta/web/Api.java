package br.com.marealta.web;

import br.com.marealta.domain.AppException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.event.Message;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.ReservationService.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Api {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private final ReservationService reservations;
    private final ReportService reports;

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin build() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(AppException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, reservations.rooms().stream().map(Api::roomJson).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(reservations.create(parseNew(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(reservations.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, reservationJson(reservations.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, reservationJson(reservations.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, reservationJson(reservations.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, receiptJson(reservations.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, receiptJson(reservations.document(ctx.pathParam("id")))));
        app.get("/messages", ctx -> json(ctx, 200, reservations.messages().stream().map(Api::messageJson).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"), "date");
            csv(ctx, reports.occupancy(date));
        });
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            YearMonth month;
            try {
                if (m == null || !m.matches("\\d{4}-\\d{2}")) throw new DateTimeParseException("", "", 0);
                month = YearMonth.parse(m);
            } catch (DateTimeParseException e) {
                throw AppException.invalid("month deve estar no formato AAAA-MM");
            }
            csv(ctx, reports.revenue(month));
        });
        return app;
    }

    // ---- entrada ----

    private static NewReservation parseNew(String body) {
        JsonNode n;
        try {
            n = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            throw AppException.invalid("Corpo não é um JSON válido");
        }
        if (n == null || !n.isObject()) throw AppException.invalid("Corpo deve ser um objeto JSON");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(n, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw AppException.invalid("ratePlan inválido");
        }
        return new NewReservation(integer(n, "roomNumber"), text(n, "guestName"), text(n, "guestEmail"),
                text(n, "guestCpf"), parseDate(text(n, "checkIn"), "checkIn"), parseDate(text(n, "checkOut"), "checkOut"),
                integer(n, "guests"), plan);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank()) throw AppException.invalid("Campo " + field + " é obrigatório");
        return v.asText();
    }

    private static int integer(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isIntegralNumber() || !v.canConvertToInt()) {
            throw AppException.invalid("Campo " + field + " deve ser um número inteiro");
        }
        return v.intValue();
    }

    private static LocalDate parseDate(String s, String field) {
        try {
            if (s == null) throw new DateTimeParseException("", "", 0);
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw AppException.invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    // ---- saída ----

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

    private static Map<String, Object> receiptJson(Receipt r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", r.title());
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
        return o;
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json; charset=utf-8").result(JSON.writeValueAsBytes(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        json(ctx, status, m);
    }
}
