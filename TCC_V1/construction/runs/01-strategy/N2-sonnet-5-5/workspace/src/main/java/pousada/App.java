package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.plan.RatePlan;
import pousada.plan.RatePlans;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

    public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                                  LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                                  ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id, r.room.number(), r.guestName, r.guestEmail, r.guestCpf, r.checkIn,
                    r.checkOut, r.guests, r.plan.code(), r.quote.nights(), r.quote.subtotal(), r.quote.discount(),
                    r.quote.total(), r.status(), r.amountPaid(), r.refund());
        }
    }

    public static Javalin create(Hotel hotel) {
        Reports reports = new Reports(hotel);
        Javalin app = Javalin.create();

        app.exception(PousadaException.class, (e, ctx) -> json(ctx, e.code.status,
                Map.of("error", e.code.name(), "message", e.getMessage())));

        app.get("/rooms", ctx -> json(ctx, 200, hotel.rooms().stream()
                .map(r -> new RoomView(r.number(), r.type(), r.capacity(), r.nightlyRate())).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, ReservationView.of(createReservation(hotel, ctx.body()))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, ReservationView.of(hotel.find(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, ReservationView.of(hotel.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, ReservationView.of(hotel.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, ReservationView.of(hotel.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, hotel.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, hotel.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> json(ctx, 200, hotel.outbox().sent()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(parseDate(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(parseMonth(ctx.queryParam("month")))));
        return app;
    }

    private static Reservation createReservation(Hotel hotel, String body) {
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            throw PousadaException.invalid("O corpo da requisição não é um JSON válido");
        }
        if (json == null || !json.isObject()) {
            throw PousadaException.invalid("O corpo da requisição deve ser um objeto JSON");
        }
        int roomNumber = integer(json, "roomNumber");
        String name = text(json, "guestName");
        String email = text(json, "guestEmail");
        String cpf = text(json, "guestCpf");
        LocalDate checkIn = parseDate(text(json, "checkIn"));
        LocalDate checkOut = parseDate(text(json, "checkOut"));
        int guests = integer(json, "guests");
        String planCode = text(json, "ratePlan");
        JsonNode company = json.get("companyCode");
        String companyCode = company != null && company.isTextual() ? company.asText() : null;

        if (!checkOut.isAfter(checkIn)) throw PousadaException.invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw PousadaException.invalid("É preciso pelo menos um hóspede");
        RatePlan plan = RatePlans.find(planCode)
                .orElseThrow(() -> PousadaException.invalid("Plano de tarifa desconhecido: " + planCode));
        return hotel.create(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan, companyCode);
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw PousadaException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw PousadaException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate parseDate(String value) {
        try {
            if (value != null) return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // reported below
        }
        throw PousadaException.invalid("Data inválida, use o formato 2027-07-10");
    }

    private static YearMonth parseMonth(String value) {
        try {
            if (value != null && value.matches("\\d{4}-\\d{2}")) return YearMonth.parse(value);
        } catch (DateTimeParseException ignored) {
            // reported below
        }
        throw PousadaException.invalid("Mês inválido, use o formato 2027-07");
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        create(new Hotel(new Outbox())).start(port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim()));
    }
}
