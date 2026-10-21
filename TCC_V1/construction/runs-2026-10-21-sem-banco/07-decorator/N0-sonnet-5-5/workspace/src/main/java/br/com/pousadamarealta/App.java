package br.com.pousadamarealta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

public final class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern DAY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           String checkIn, String checkOut, int guests, String ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                           BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id, r.room.number(), r.guestName, r.guestEmail, r.guestCpf,
                    r.checkIn.toString(), r.checkOut.toString(), r.guests, r.ratePlan.name(), r.nights,
                    r.subtotal, r.discount, r.total, r.status.name(), r.amountPaid, r.refund);
        }
    }

    record DocumentView(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                        String roomType, String checkIn, String checkOut, int nights, BigDecimal subtotal,
                        BigDecimal discount, BigDecimal total) {
        static DocumentView of(Reservation r) {
            return new DocumentView("Comprovante de reserva", r.id, r.guestName, r.guestCpf, r.room.number(),
                    r.room.type().name(), r.checkIn.toString(), r.checkOut.toString(), r.nights,
                    r.subtotal, r.discount, r.total);
        }
    }

    private App() {}

    public static Javalin create(AppConfig config, Clock clock) {
        MessageLog log = new MessageLog();
        MessageDispatcher dispatcher = new MessageDispatcher(config, clock, log);
        ReservationService service = new ReservationService(dispatcher);

        Javalin app = Javalin.create();

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Unexpected error");
        });

        app.get("/rooms", ctx -> json(ctx, 200, Room.ALL));
        app.post("/reservations", ctx -> json(ctx, 201, ReservationView.of(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, DocumentView.of(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, DocumentView.of(service.forDocument(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, log.all()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                Reports.occupancy(service.snapshot(), parseDay(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                Reports.revenue(service.snapshot(), parseMonth(ctx.queryParam("month")))));
        return app;
    }

    private static void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(body));
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message == null ? code : message));
        } catch (Exception e) {
            ctx.status(status).result(code);
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reservation not found: " + ctx.pathParam("id"));
        }
    }

    private static LocalDate parseDay(String value) {
        if (value == null || !DAY.matcher(value).matches()) {
            throw ApiException.invalid("date must be in the format yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("date must be in the format yyyy-MM-dd");
        }
    }

    private static YearMonth parseMonth(String value) {
        if (value == null || !MONTH.matcher(value).matches()) {
            throw ApiException.invalid("month must be in the format yyyy-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month must be in the format yyyy-MM");
        }
    }

    private static ReservationService.NewReservation parseNew(Context ctx) {
        JsonNode root;
        try {
            root = MAPPER.readTree(ctx.body());
        } catch (Exception e) {
            throw ApiException.invalid("Body is not valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw ApiException.invalid("Body must be a JSON object");
        }
        Integer room = roomNumber(root.get("roomNumber"));
        String name = text(root, "guestName");
        String email = text(root, "guestEmail");
        String cpf = text(root, "guestCpf");
        LocalDate in = date(root, "checkIn");
        LocalDate out = date(root, "checkOut");
        JsonNode g = root.get("guests");
        if (g == null || !g.isIntegralNumber() || !g.canConvertToInt()) {
            throw ApiException.invalid("guests must be an integer");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(root, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan must be STANDARD or NON_REFUNDABLE");
        }
        return new ReservationService.NewReservation(room, name, email, cpf, in, out, g.intValue(), plan);
    }

    private static Integer roomNumber(JsonNode n) {
        if (n != null && n.isIntegralNumber() && n.canConvertToInt()) return n.intValue();
        if (n != null && n.isTextual() && n.asText().matches("\\d{1,9}")) return Integer.parseInt(n.asText());
        throw ApiException.invalid("roomNumber must be a room number");
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid(field + " is required");
        }
        return n.asText();
    }

    private static LocalDate date(JsonNode root, String field) {
        String v = text(root, field);
        if (!DAY.matcher(v).matches()) throw ApiException.invalid(field + " must be in the format yyyy-MM-dd");
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " must be in the format yyyy-MM-dd");
        }
    }
}
