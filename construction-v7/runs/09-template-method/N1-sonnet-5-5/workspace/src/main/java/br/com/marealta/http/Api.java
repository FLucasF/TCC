package br.com.marealta.http;

import br.com.marealta.ApiException;
import br.com.marealta.Message;
import br.com.marealta.Outbox;
import br.com.marealta.RatePlan;
import br.com.marealta.Receipt;
import br.com.marealta.Reservation;
import br.com.marealta.ReservationService;
import br.com.marealta.Room;
import br.com.marealta.reports.Report;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Api {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ReservationService reservations;
    private final Outbox outbox;
    private final List<Report> reports;

    public Api(ReservationService reservations, Outbox outbox, List<Report> reports) {
        this.reservations = reservations;
        this.outbox = outbox;
        this.reports = reports;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create();
        routes(app);
        return app.start(port);
    }

    private void routes(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> ctx.status(e.status())
                .json(Map.of("error", e.code(), "message", e.getMessage())));

        app.get("/rooms", ctx -> ctx.json(reservations.rooms().stream().map(Api::roomJson).toList()));

        app.post("/reservations", ctx -> ctx.status(201).json(reservationJson(create(ctx))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservationJson(reservations.find(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservationJson(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservationJson(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservationJson(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(receiptJson(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(receiptJson(reservations.document(id(ctx)))));

        app.get("/messages", ctx -> ctx.json(outbox.all().stream().map(Api::messageJson).toList()));

        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> ctx
                    .contentType("text/csv; charset=utf-8")
                    .result(report.render(ctx.queryParam(report.paramName()))));
        }
    }

    private Reservation create(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw ApiException.invalid("O corpo deve ser um objeto JSON");
        }
        return reservations.create(
                integer(body, "roomNumber"),
                text(body, "guestName"),
                text(body, "guestEmail"),
                text(body, "guestCpf"),
                date(body, "checkIn"),
                date(body, "checkOut"),
                integer(body, "guests"),
                ratePlan(body, "ratePlan"));
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            return -1; // vira RESERVATION_NOT_FOUND
        }
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato yyyy-MM-dd");
        }
    }

    private static RatePlan ratePlan(JsonNode body, String field) {
        try {
            return RatePlan.valueOf(text(body, field));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido");
        }
    }

    private static Map<String, Object> roomJson(Room room) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("number", room.number());
        json.put("type", room.type());
        json.put("capacity", room.capacity());
        json.put("nightlyRate", room.nightlyRate());
        return json;
    }

    private static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", r.id());
        json.put("roomNumber", r.room().number());
        json.put("guestName", r.guestName());
        json.put("guestEmail", r.guestEmail());
        json.put("guestCpf", r.guestCpf());
        json.put("checkIn", r.checkIn().toString());
        json.put("checkOut", r.checkOut().toString());
        json.put("guests", r.guests());
        json.put("ratePlan", r.ratePlan());
        json.put("nights", r.nights());
        json.put("subtotal", r.subtotal());
        json.put("discount", r.discount());
        json.put("total", r.total());
        json.put("status", r.status());
        json.put("amountPaid", r.amountPaid());
        json.put("refund", r.refund());
        return json;
    }

    private static Map<String, Object> receiptJson(Receipt d) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", d.title());
        json.put("reservationId", d.reservationId());
        json.put("guestName", d.guestName());
        json.put("guestCpf", d.guestCpf());
        json.put("roomNumber", d.roomNumber());
        json.put("roomType", d.roomType());
        json.put("checkIn", d.checkIn().toString());
        json.put("checkOut", d.checkOut().toString());
        json.put("nights", d.nights());
        json.put("subtotal", d.subtotal());
        json.put("discount", d.discount());
        json.put("total", d.total());
        return json;
    }

    private static Map<String, Object> messageJson(Message m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", m.id());
        json.put("channel", m.channel());
        json.put("to", m.to());
        json.put("subject", m.subject());
        json.put("body", m.body());
        return json;
    }
}
