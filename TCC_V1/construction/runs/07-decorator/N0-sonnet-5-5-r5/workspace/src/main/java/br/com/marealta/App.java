package br.com.marealta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

public final class App {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    private static final Set<String> RATE_PLANS = Set.of("STANDARD", "NON_REFUNDABLE");

    public static void main(String[] args) throws Exception {
        Config cfg = Config.fromEnv(System.getenv());
        Db db = Db.connect(cfg);
        MessageService messages = new MessageService(new MessagePolicy(cfg.messages()), Clock.system(cfg.zone()));
        Javalin app = create(db, messages);
        app.start(cfg.port());
    }

    public static Javalin create(Db db, MessageService messages) {
        ReservationService reservations = new ReservationService(db, messages);
        ReportService reports = new ReportService(db);

        Javalin app = Javalin.create(c -> {
            c.jsonMapper(new JavalinJackson(MAPPER, false));
            c.showJavalinBanner = false;
        });

        app.get("/rooms", ctx -> ctx.json(reservations.rooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(reservations.create(parseNew(ctx))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(db.tx(messages::list)));
        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"), "date");
            ctx.contentType("text/csv; charset=utf-8").result(reports.occupancy(date));
        });
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            YearMonth month;
            try {
                if (m == null) throw new DateTimeParseException("", "", 0);
                month = YearMonth.parse(m);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("month deve estar no formato 2027-07");
            }
            ctx.contentType("text/csv; charset=utf-8").result(reports.revenue(month));
        });

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }

    private static LocalDate parseDate(String s, String field) {
        try {
            if (s == null) throw new DateTimeParseException("", "", 0);
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato 2027-07-10");
        }
    }

    private static ReservationService.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("Corpo deve ser um objeto JSON");

        JsonNode room = body.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("roomNumber é obrigatório e deve ser inteiro");
        }
        JsonNode guests = body.get("guests");
        if (guests == null || !guests.isIntegralNumber() || !guests.canConvertToInt()) {
            throw ApiException.invalid("guests é obrigatório e deve ser inteiro");
        }
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate in = parseDate(text(body, "checkIn"), "checkIn");
        LocalDate out = parseDate(text(body, "checkOut"), "checkOut");
        String plan = text(body, "ratePlan");
        if (!RATE_PLANS.contains(plan)) throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        if (!out.isAfter(in)) throw ApiException.invalid("checkOut deve ser depois de checkIn");
        if (guests.asInt() < 1) throw ApiException.invalid("guests deve ser pelo menos 1");
        return new ReservationService.NewReservation(room.asInt(), name, email, cpf, in, out, guests.asInt(), plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid(field + " é obrigatório");
        }
        return n.asText();
    }
}
