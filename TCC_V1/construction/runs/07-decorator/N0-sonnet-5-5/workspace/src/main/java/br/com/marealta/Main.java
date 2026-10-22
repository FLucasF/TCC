package br.com.marealta;

import br.com.marealta.Domain.RatePlan;
import br.com.marealta.Domain.Reservation;
import br.com.marealta.Domain.Room;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern DAY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public static void main(String[] args) {
        Config config = Config.fromEnv(System.getenv());
        if (config.dbUrl() == null) {
            throw new IllegalStateException("DB_URL não informado");
        }
        Database db = new Database(config.dbUrl(), config.dbUser(), config.dbPassword());
        db.migrate();
        Service service = new Service(db, Clock.systemUTC(), config);
        Javalin app = create(service);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            db.close();
        }));
        app.start("0.0.0.0", config.port());
        log.info("Pousada Maré Alta no ar na porta {} (modo de teste: {})", config.port(), config.testMode());
    }

    public static Javalin create(Service service) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty() || "Not found".equalsIgnoreCase(ctx.result())) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
            }
        });

        app.get("/rooms", ctx -> {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Room r : service.rooms()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("number", r.number());
                m.put("type", r.type());
                m.put("capacity", r.capacity());
                m.put("nightlyRate", r.nightlyRate());
                out.add(m);
            }
            json(ctx, 200, out);
        });

        app.post("/reservations", ctx -> json(ctx, 201, view(service.create(parseNew(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, view(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, view(service.document(id(ctx)))));

        app.get("/messages", ctx -> {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Service.StoredMessage m : service.messages()) {
                Map<String, Object> v = new LinkedHashMap<>();
                v.put("id", m.id());
                v.put("channel", m.channel());
                v.put("to", m.to());
                v.put("subject", m.subject());
                v.put("body", m.body());
                v.put("status", m.status());
                out.add(v);
            }
            json(ctx, 200, out);
        });

        app.get("/reports/occupancy", ctx -> {
            String d = ctx.queryParam("date");
            if (d == null || !DAY.matcher(d).matches()) throw ApiException.invalid("Informe date no formato AAAA-MM-DD");
            csv(ctx, service.occupancyCsv(date(d, "date")));
        });
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            if (m == null || !MONTH.matcher(m).matches()) throw ApiException.invalid("Informe month no formato AAAA-MM");
            try {
                csv(ctx, service.revenueCsv(YearMonth.parse(m)));
            } catch (DateTimeException e) {
                throw ApiException.invalid("month inválido");
            }
        });
        return app;
    }

    // ---------- entrada e saída ----------

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
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

    private static LocalDate date(String s, String field) {
        if (s == null || !DAY.matcher(s).matches()) throw ApiException.invalid(field + " deve estar no formato AAAA-MM-DD");
        try {
            return LocalDate.parse(s);
        } catch (DateTimeException e) {
            throw ApiException.invalid(field + " é uma data inválida");
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText();
    }

    private static Service.NewReservation parseNew(String body) {
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (root == null || !root.isObject()) throw ApiException.invalid("Corpo deve ser um objeto JSON");

        JsonNode roomN = root.get("roomNumber");
        JsonNode guestsN = root.get("guests");
        if (roomN == null || !roomN.isIntegralNumber() || !roomN.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: roomNumber");
        }
        if (guestsN == null || !guestsN.isIntegralNumber() || !guestsN.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: guests");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(root, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        String name = text(root, "guestName");
        String email = text(root, "guestEmail");
        String cpf = text(root, "guestCpf");
        LocalDate in = date(text(root, "checkIn"), "checkIn");
        LocalDate out = date(text(root, "checkOut"), "checkOut");
        if (!out.isAfter(in)) throw ApiException.invalid("checkOut deve ser depois de checkIn");
        if (guestsN.asInt() < 1) throw ApiException.invalid("guests deve ser pelo menos 1");
        return new Service.NewReservation(roomN.asInt(), name, email, cpf, in, out, guestsN.asInt(), plan);
    }

    private static Map<String, Object> view(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.roomNumber());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        m.put("guestCpf", r.guestCpf());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
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

    private static Map<String, Object> view(Receipt d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", Receipt.TITLE);
        m.put("reservationId", d.reservationId());
        m.put("guestName", d.guestName());
        m.put("guestCpf", d.guestCpf());
        m.put("roomNumber", d.roomNumber());
        m.put("roomType", d.roomType());
        m.put("checkIn", d.checkIn().toString());
        m.put("checkOut", d.checkOut().toString());
        m.put("nights", d.nights());
        m.put("subtotal", d.subtotal());
        m.put("discount", d.discount());
        m.put("total", d.total());
        return m;
    }
}
