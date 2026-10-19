package br.com.marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class App {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private final ReservationService service;
    private final NotificationService notifications;

    public App(boolean testMode) {
        this.notifications = new NotificationService(testMode);
        this.service = new ReservationService(notifications);
    }

    public Javalin build() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno"));

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(App::roomJson).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(service.create(parseCreate(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, reservationJson(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, reservationJson(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, reservationJson(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, documentJson(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, documentJson(service.paidReservation(ctx.pathParam("id")))));

        app.get("/messages", ctx -> json(ctx, 200, notifications.messages().stream().map(App::messageJson).toList()));

        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"), "date");
            csv(ctx, service.occupancyCsv(date));
        });
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            if (m == null || m.isBlank()) throw ApiException.invalid("Informe o mês (month=AAAA-MM)");
            YearMonth month;
            try {
                month = YearMonth.parse(m);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Mês inválido: " + m);
            }
            csv(ctx, service.revenueCsv(month));
        });
        return app;
    }

    // ---- entrada ----

    private static ReservationService.CreateCommand parseCreate(Context ctx) {
        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");

        int room = intField(body, "roomNumber");
        String name = textField(body, "guestName");
        String email = textField(body, "guestEmail");
        String cpf = textField(body, "guestCpf");
        LocalDate in = parseDate(textField(body, "checkIn"), "checkIn");
        LocalDate out = parseDate(textField(body, "checkOut"), "checkOut");
        int guests = intField(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(textField(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("Plano de tarifa inválido");
        }
        return new ReservationService.CreateCommand(room, name, email, cpf, in, out, guests, plan);
    }

    private static String textField(JsonNode body, String name) {
        JsonNode n = body.get(name);
        if (n == null || n.isNull()) throw ApiException.invalid("Campo obrigatório: " + name);
        if (!n.isTextual() || n.asText().isBlank()) throw ApiException.invalid("Campo inválido: " + name);
        return n.asText();
    }

    private static int intField(JsonNode body, String name) {
        JsonNode n = body.get(name);
        if (n == null || n.isNull()) throw ApiException.invalid("Campo obrigatório: " + name);
        if (!n.isIntegralNumber() || !n.canConvertToInt()) throw ApiException.invalid("Campo inválido: " + name);
        return n.asInt();
    }

    private static LocalDate parseDate(String s, String field) {
        if (s == null || s.isBlank()) throw ApiException.invalid("Informe a data: " + field);
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida em " + field + ": " + s);
        }
    }

    // ---- saída ----

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
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
        m.put("id", r.id);
        m.put("roomNumber", r.room.number());
        m.put("guestName", r.guestName);
        m.put("guestEmail", r.guestEmail);
        m.put("guestCpf", r.guestCpf);
        m.put("checkIn", r.checkIn.toString());
        m.put("checkOut", r.checkOut.toString());
        m.put("guests", r.guests);
        m.put("ratePlan", r.ratePlan);
        m.put("nights", r.nights);
        m.put("subtotal", r.subtotal);
        m.put("discount", r.discount);
        m.put("total", r.total);
        m.put("status", r.status);
        m.put("amountPaid", r.amountPaid);
        m.put("refund", r.refund);
        return m;
    }

    private static Map<String, Object> documentJson(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", "Comprovante de reserva");
        m.put("reservationId", r.id);
        m.put("guestName", r.guestName);
        m.put("guestCpf", r.guestCpf);
        m.put("roomNumber", r.room.number());
        m.put("roomType", r.room.type());
        m.put("checkIn", r.checkIn.toString());
        m.put("checkOut", r.checkOut.toString());
        m.put("nights", r.nights);
        m.put("subtotal", r.subtotal);
        m.put("discount", r.discount);
        m.put("total", r.total);
        return m;
    }

    private static Map<String, Object> messageJson(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        m.put("status", msg.status());
        return m;
    }
}
