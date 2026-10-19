package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.event.EventBus;
import br.com.marealta.pousada.notification.MessageLog;
import br.com.marealta.pousada.notification.Notifications;
import br.com.marealta.pousada.service.DomainException;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.service.ReservationService.NewReservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Api {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageLog messages;

    public Api() {
        EventBus bus = new EventBus();
        this.messages = new MessageLog();
        this.reservations = new ReservationService(bus);
        this.reports = new ReportService(reservations);
        Notifications.register(bus, messages);
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno"));

        app.get("/rooms", ctx -> json(ctx, 200, reservations.rooms().stream().map(Api::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, view(reservations.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, messages.all()));

        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));
        return app;
    }

    // ---- views ----

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
        m.put("roomNumber", r.room().number());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.put("guestCpf", r.guestCpf());
        m.put("checkIn", r.checkIn());
        m.put("checkOut", r.checkOut());
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

    // ---- request parsing ----

    private static NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw DomainException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw DomainException.invalid("Corpo da requisição deve ser um objeto JSON");
        }
        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = parseDate(text(body, "checkIn"), "checkIn");
        LocalDate checkOut = parseDate(text(body, "checkOut"), "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw DomainException.invalid("ratePlan inválido");
        }
        return new NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw DomainException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw DomainException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.intValue();
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null) {
            throw DomainException.invalid("Parâmetro obrigatório: " + field);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Data inválida em " + field + " (use aaaa-mm-dd)");
        }
    }

    private static YearMonth parseMonth(String value) {
        if (value == null) {
            throw DomainException.invalid("Parâmetro obrigatório: month");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Mês inválido (use aaaa-mm)");
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }

    // ---- responses ----

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
