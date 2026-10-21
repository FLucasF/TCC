package pousada;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.identity.IdentityKinds;

public final class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final Outbox outbox;

    public App(ReservationService service, Outbox outbox) {
        this.service = service;
        this.outbox = outbox;
    }

    public static Javalin create() {
        Outbox outbox = new Outbox();
        return new App(new ReservationService(outbox), outbox).routes();
    }

    private Javalin routes() {
        Javalin app = Javalin.create();
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno."));

        app.get("/rooms", ctx -> ctx.json(RoomCatalog.all().stream().map(App::roomView).toList()));
        app.post("/reservations", ctx -> {
            Reservation r = ReservationRequest.parse(readBody(ctx), service);
            ctx.status(201).json(reservationView(r));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(reservationView(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservationView(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservationView(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservationView(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(documentView(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(documentView(service.paidReservation(id(ctx)))));
        app.get("/messages", ctx -> ctx.json(outbox.all().stream().map(App::messageView).toList()));
        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseOrInvalid(ctx.queryParam("date"), LocalDate::parse, "date");
            ctx.contentType(CSV).result(Reports.occupancy(service.all(), date));
        });
        app.get("/reports/revenue", ctx -> {
            YearMonth month = parseOrInvalid(ctx.queryParam("month"), YearMonth::parse, "month");
            ctx.contentType(CSV).result(Reports.revenue(service.all(), month));
        });
        return app;
    }

    private static <T> T parseOrInvalid(String value, java.util.function.Function<String, T> parser, String name) {
        if (value == null) {
            throw ApiException.invalid("Informe " + name + ".");
        }
        try {
            return parser.apply(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Valor inválido para " + name + ".");
        }
    }

    private static JsonNode readBody(Context ctx) {
        try {
            JsonNode body = MAPPER.readTree(ctx.body());
            if (body == null || !body.isObject()) {
                throw ApiException.invalid("O corpo deve ser um objeto JSON.");
            }
            return body;
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido.");
        }
    }

    private static int id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + raw + " não existe.");
        }
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static Map<String, Object> roomView(Room room) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("number", room.number());
        view.put("type", room.type());
        view.put("capacity", room.capacity());
        view.put("nightlyRate", room.nightlyRate());
        return view;
    }

    private static Map<String, Object> reservationView(Reservation r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", r.id());
        view.put("roomNumber", r.room().number());
        view.put("guestName", r.guestName());
        view.put("guestEmail", r.guestEmail());
        view.putAll(IdentityKinds.toJsonFields(r.identity()));
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("guests", r.guests());
        view.put("ratePlan", r.ratePlan());
        view.put("nights", r.quote().nights());
        view.put("subtotal", r.quote().subtotal());
        view.put("discount", r.quote().discount());
        view.put("total", r.quote().total());
        view.put("status", r.status());
        view.put("amountPaid", r.amountPaid());
        view.put("refund", r.refund());
        return view;
    }

    private static Map<String, Object> documentView(Reservation r) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", r.identity().documentTitle());
        view.put("reservationId", r.id());
        view.put("guestName", r.guestName());
        view.putAll(IdentityKinds.toJsonFields(r.identity()));
        view.put("roomNumber", r.room().number());
        view.put("roomType", r.room().type());
        view.put("checkIn", r.checkIn().toString());
        view.put("checkOut", r.checkOut().toString());
        view.put("nights", r.quote().nights());
        view.put("subtotal", r.quote().subtotal());
        view.put("discount", r.quote().discount());
        view.put("total", r.quote().total());
        view.put("text", ReservationService.documentText(r));
        return view;
    }

    private static Map<String, Object> messageView(Message m) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", m.id());
        view.put("channel", m.channel());
        view.put("to", m.to());
        view.put("subject", m.subject());
        view.put("body", m.body());
        return view;
    }
}
