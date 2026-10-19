package com.marealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ReservationService service;
    private final Outbox outbox = new Outbox();
    private final Reports reports;

    public App() {
        service = new ReservationService(List.of(
                new Room(101, RoomType.STANDARD), new Room(102, RoomType.STANDARD),
                new Room(201, RoomType.DELUXE), new Room(202, RoomType.DELUXE),
                new Room(301, RoomType.SUITE)));
        // Para avisar mais alguém (restaurante, transfer...), registre aqui um novo ReservationListener.
        service.addListener(new GuestNotifier(outbox));
        service.addListener(new HousekeepingNotifier(outbox));
        service.addListener(new FinanceNotifier(outbox));
        reports = new Reports(service);
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create();
        app.exception(ApiException.class, (e, ctx) -> {
            ctx.status(e.status());
            json(ctx, Map.of("error", e.code(), "message", e.getMessage()));
        });
        app.get("/rooms", ctx -> json(ctx, service.rooms().stream().map(RoomView::of).toList()));
        app.post("/reservations", ctx -> {
            ctx.status(201);
            json(ctx, ReservationView.of(create(ctx.body())));
        });
        app.get("/reservations/{id}", ctx -> json(ctx, ReservationView.of(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, ReservationView.of(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, ReservationView.of(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, ReservationView.of(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, outbox.all()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));
        return app.start(port);
    }

    private Reservation create(String body) {
        JsonNode node;
        try {
            node = MAPPER.readTree(body);
        } catch (Exception e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (node == null || !node.isObject()) throw ApiException.invalid("Corpo deve ser um objeto JSON");
        int roomNumber = integer(node, "roomNumber");
        String name = text(node, "guestName");
        String email = text(node, "guestEmail");
        String cpf = text(node, "guestCpf");
        LocalDate checkIn = parseDate(text(node, "checkIn"), "checkIn");
        LocalDate checkOut = parseDate(text(node, "checkOut"), "checkOut");
        int guests = integer(node, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(node, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido");
        }
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");
        return service.create(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank()) throw ApiException.invalid("Campo obrigatório: " + field);
        return v.asText();
    }

    private static int integer(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isIntegralNumber() || !v.canConvertToInt()) throw ApiException.invalid("Campo obrigatório (inteiro): " + field);
        return v.asInt();
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null) throw ApiException.invalid("Campo obrigatório: " + field);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida em " + field + ", use AAAA-MM-DD");
        }
    }

    private static YearMonth parseMonth(String value) {
        if (value == null) throw ApiException.invalid("Campo obrigatório: month");
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Mês inválido, use AAAA-MM");
        }
    }

    private static void json(Context ctx, Object body) {
        try {
            ctx.contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.contentType("text/csv; charset=utf-8").result(body);
    }
}
