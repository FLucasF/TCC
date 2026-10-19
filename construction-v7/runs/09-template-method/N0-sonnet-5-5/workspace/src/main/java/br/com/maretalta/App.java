package br.com.maretalta;

import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Room;
import br.com.maretalta.report.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ReservationService service;
    private final Outbox outbox;
    private final List<Report> reports;

    public App(TourismTaxPolicy taxPolicy) {
        this.outbox = new Outbox();
        this.service = new ReservationService(outbox);
        // To add a report, implement Report and list it here.
        this.reports = List.of(
                new OccupancyReport(service),
                new RevenueReport(service),
                new TourismTaxReport(service, taxPolicy),
                new RatePlanReport(service));
    }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Unexpected error"));

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(App::roomJson).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(service.create(parseRequest(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationJson(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationJson(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationJson(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx ->
                json(ctx, 200, documentJson(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document", ctx ->
                json(ctx, 200, documentJson(service.receiptFor(ctx.pathParam("id")))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.all().stream().map(App::messageJson).toList()));

        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = report.generate(ctx.queryParam(report.paramName()));
                ctx.status(200).contentType("text/csv; charset=utf-8").result(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            });
        }
        return app;
    }

    // ---- request parsing ----

    private static ReservationService.Request parseRequest(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.bodyAsBytes());
        } catch (Exception e) {
            throw ApiException.invalid("Body must be valid JSON");
        }
        if (body == null || !body.isObject()) {
            throw ApiException.invalid("Body must be a JSON object");
        }
        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan must be STANDARD or NON_REFUNDABLE");
        }
        return new ReservationService.Request(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid(field + " is required");
        }
        return n.asText().trim();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw ApiException.invalid(field + " must be an integer");
        }
        return n.intValue();
    }

    private static LocalDate date(JsonNode body, String field) {
        String raw = text(body, field);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " must be in the format yyyy-MM-dd");
        }
    }

    // ---- responses ----

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
    }

    private static void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (Exception e) {
            ctx.status(500);
        }
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
        m.put("title", ReceiptFormatter.TITLE);
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

    private static Map<String, Object> messageJson(Outbox.Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        return m;
    }
}
