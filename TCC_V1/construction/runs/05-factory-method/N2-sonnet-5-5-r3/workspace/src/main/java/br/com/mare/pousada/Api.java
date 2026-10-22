package br.com.mare.pousada;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Api {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ReservationService service;
    private final MessageRepository messages;
    private final Reports reports;

    public Api(ReservationService service, MessageRepository messages, Reports reports) {
        this.service = service;
        this.messages = messages;
        this.reports = reports;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create();
        app.exception(PousadaException.class, (e, ctx) -> json(ctx, e.code().status(), Map.of(
                "error", e.code().name(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> json(ctx, 500, Map.of(
                "error", "INTERNAL_ERROR", "message", "Erro interno")));

        app.get("/rooms", ctx -> json(ctx, 200, RoomCatalog.all().stream().map(Api::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationView(service.create(parseBooking(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationView(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, reservationView(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, reservationView(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, reservationView(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, documentView(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, documentView(service.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, messages.findAll().stream().map(Api::messageView).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(month(ctx.queryParam("month")))));
        return app.start(port);
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json; charset=utf-8").result(JSON.writeValueAsString(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }

    private static LocalDate date(String text) {
        try {
            if (text == null) throw invalid("Data ausente");
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida: " + text);
        }
    }

    private static YearMonth month(String text) {
        try {
            if (text == null) throw invalid("Mês ausente");
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            throw invalid("Mês inválido: " + text);
        }
    }

    @SuppressWarnings("unchecked")
    private static BookingRequest parseBooking(Context ctx) {
        Map<String, Object> body;
        try {
            Object parsed = JSON.readValue(ctx.body(), Object.class);
            if (!(parsed instanceof Map)) throw invalid("O corpo deve ser um objeto JSON");
            body = (Map<String, Object>) parsed;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw invalid("Corpo não é um JSON válido");
        }
        if (!(body.get("roomNumber") instanceof Integer room)) throw invalid("roomNumber ausente ou inválido");
        if (!(body.get("guests") instanceof Integer guests)) throw invalid("guests ausente ou inválido");
        if (guests < 1) throw invalid("É preciso pelo menos um hóspede");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        GuestIdentity identity = IdentityKinds.fromBody(body);
        LocalDate checkIn = date(text(body, "checkIn"));
        LocalDate checkOut = date(text(body, "checkOut"));
        if (!checkOut.isAfter(checkIn)) throw invalid("A saída deve ser depois da entrada");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan inválido");
        }
        return new BookingRequest(room, name, email, identity, checkIn, checkOut, guests, plan);
    }

    private static String text(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof String s) || s.isBlank()) throw invalid(field + " ausente ou inválido");
        return s;
    }

    private static Map<String, Object> identityView(GuestIdentity identity) {
        Map<String, Object> view = new LinkedHashMap<>();
        for (String field : IdentityKinds.allFieldNames()) {
            view.put(field, identity.values().get(field));
        }
        return view;
    }

    private static Map<String, Object> roomView(Room room) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("number", room.number());
        v.put("type", room.type());
        v.put("capacity", room.capacity());
        v.put("nightlyRate", Money.round(room.type().nightlyRate()));
        return v;
    }

    private static Map<String, Object> reservationView(Reservation r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", r.id());
        v.put("roomNumber", r.room().number());
        v.put("guestName", r.guestName());
        v.put("guestEmail", r.guestEmail());
        v.putAll(identityView(r.identity()));
        v.put("checkIn", r.checkIn().toString());
        v.put("checkOut", r.checkOut().toString());
        v.put("guests", r.guests());
        v.put("ratePlan", r.ratePlan());
        v.put("nights", r.quote().nights());
        v.put("subtotal", r.quote().subtotal());
        v.put("discount", r.quote().discount());
        v.put("total", r.quote().total());
        v.put("status", r.status());
        v.put("amountPaid", r.amountPaid());
        v.put("refund", r.refund());
        return v;
    }

    private static Map<String, Object> documentView(Document d) {
        Reservation r = d.reservation();
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("title", d.title());
        v.put("reservationId", r.id());
        v.put("guestName", r.guestName());
        v.putAll(identityView(r.identity()));
        v.put("roomNumber", r.room().number());
        v.put("roomType", r.room().type());
        v.put("checkIn", r.checkIn().toString());
        v.put("checkOut", r.checkOut().toString());
        v.put("nights", r.quote().nights());
        v.put("subtotal", r.quote().subtotal());
        v.put("discount", r.quote().discount());
        v.put("total", r.quote().total());
        v.put("text", d.text());
        return v;
    }

    private static Map<String, Object> messageView(Message m) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", m.id());
        v.put("channel", m.channel());
        v.put("to", m.to());
        v.put("subject", m.subject());
        v.put("body", m.body());
        return v;
    }
}
