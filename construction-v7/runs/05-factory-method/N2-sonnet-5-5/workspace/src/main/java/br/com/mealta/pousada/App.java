package br.com.mealta.pousada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Outbox outbox = new Outbox();
    private final ReservationService service = new ReservationService(outbox);

    public static Javalin create() {
        return new App().routes();
    }

    private Javalin routes() {
        Javalin app = Javalin.create();
        app.exception(DomainException.class, (e, ctx) -> error(ctx, e));
        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(App::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationView(create(ctx))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationView(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationView(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationView(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationView(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, documentView(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, documentView(service.paidReservation(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.all().stream().map(App::messageView).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                Reports.occupancy(date(ctx.queryParam("date")), service.all(), service.rooms())));
        app.get("/reports/revenue", ctx -> csv(ctx, Reports.revenue(month(ctx.queryParam("month")), service.all())));
        return app;
    }

    private Reservation create(Context ctx) {
        Map<String, Object> body = parseBody(ctx.body());
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        Identity identity = IdentityTypes.parse(body);
        LocalDate checkIn = date(text(body, "checkIn"));
        LocalDate checkOut = date(text(body, "checkOut"));
        if (!checkOut.isAfter(checkIn)) {
            throw DomainException.invalid("A saída deve ser depois da entrada");
        }
        int guests = integer(body, "guests");
        if (guests < 1) {
            throw DomainException.invalid("É preciso pelo menos um hóspede");
        }
        int roomNumber = integer(body, "roomNumber");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw DomainException.invalid("Plano de tarifa inválido");
        }
        return service.create(roomNumber, name, email, identity, checkIn, checkOut, guests, plan);
    }

    private static Map<String, Object> parseBody(String raw) {
        try {
            Map<String, Object> body = JSON.readValue(raw, new TypeReference<>() {});
            if (body == null) throw DomainException.invalid("Corpo da requisição ausente");
            return body;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw DomainException.invalid("Corpo da requisição não é um JSON válido");
        }
    }

    private static Object required(Map<String, Object> body, String name) {
        Object v = body.get(name);
        if (v == null) throw DomainException.invalid("Campo obrigatório ausente: " + name);
        return v;
    }

    private static String text(Map<String, Object> body, String name) {
        if (!(required(body, name) instanceof String s) || s.isBlank()) {
            throw DomainException.invalid("Campo " + name + " deve ser um texto não vazio");
        }
        return s.trim();
    }

    private static int integer(Map<String, Object> body, String name) {
        if (required(body, name) instanceof Integer i) return i;
        throw DomainException.invalid("Campo " + name + " deve ser um número inteiro");
    }

    private static LocalDate date(String value) {
        if (value == null) throw DomainException.invalid("Data ausente");
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Data inválida, use AAAA-MM-DD: " + value);
        }
    }

    private static YearMonth month(String value) {
        if (value == null) throw DomainException.invalid("Mês ausente");
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Mês inválido, use AAAA-MM: " + value);
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
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

    private static Map<String, Object> reservationView(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.room().number());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.putAll(IdentityTypes.view(r.identity()));
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("guests", r.guests());
        m.put("ratePlan", r.ratePlan());
        m.put("nights", r.quote().nights());
        m.put("subtotal", r.quote().subtotal());
        m.put("discount", r.quote().discount());
        m.put("total", r.quote().total());
        m.put("status", r.status());
        m.put("amountPaid", r.amountPaid());
        m.put("refund", r.refund());
        return m;
    }

    private static Map<String, Object> documentView(Reservation r) {
        DocumentData d = r.documentData();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", r.identity().title());
        m.put("reservationId", d.reservationId());
        m.put("guestName", d.guestName());
        m.putAll(IdentityTypes.view(r.identity()));
        m.put("roomNumber", d.roomNumber());
        m.put("roomType", d.roomType());
        m.put("checkIn", d.checkIn().toString());
        m.put("checkOut", d.checkOut().toString());
        m.put("nights", d.nights());
        m.put("subtotal", d.subtotal());
        m.put("discount", d.discount());
        m.put("total", d.total());
        m.put("text", Documents.text(r));
        return m;
    }

    private static Map<String, Object> messageView(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        return m;
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json; charset=utf-8").result(JSON.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, DomainException e) {
        json(ctx, e.status(), Map.of("error", e.code(), "message", e.getMessage()));
    }
}
