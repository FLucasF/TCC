package pousada;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class Api {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final ReservationService service;

    public Api(ReservationService service) {
        this.service = service;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, Room.ALL.stream().map(Api::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationView(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationView(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationView(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationView(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationView(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, documentView(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, documentView(service.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, service.messages().stream().map(Api::messageView).toList()));
        app.get("/reports/occupancy", ctx -> {
            LocalDate date = parseDate(ctx.queryParam("date"), "date");
            csv(ctx, Reports.occupancy(service.occupying(date)));
        });
        app.get("/reports/revenue", ctx -> {
            String raw = ctx.queryParam("month");
            if (raw == null || !MONTH.matcher(raw).matches()) throw ApiException.invalid("month deve ser AAAA-MM");
            YearMonth month;
            try {
                month = YearMonth.parse(raw);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("month deve ser AAAA-MM");
            }
            csv(ctx, Reports.revenue(service.startingBetween(month.atDay(1), month.plusMonths(1).atDay(1))));
        });
        return app.start(port);
    }

    // ---- request parsing ----

    private static ReservationService.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("Corpo deve ser um objeto JSON");

        String guestName = text(body, "guestName");
        String guestEmail = text(body, "guestEmail");
        int roomNumber = integer(body, "roomNumber");
        int guests = integer(body, "guests");
        LocalDate checkIn = parseDate(optionalText(body, "checkIn"), "checkIn");
        LocalDate checkOut = parseDate(optionalText(body, "checkOut"), "checkOut");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido");
        }
        GuestIdentity identity = identity(body);
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("checkOut deve ser depois de checkIn");
        if (guests < 1) throw ApiException.invalid("guests deve ser pelo menos 1");
        return new ReservationService.NewReservation(roomNumber, guestName, guestEmail, identity, checkIn, checkOut,
                guests, plan);
    }

    /** Exactly one of: guestCpf | companyCnpj+companyName | guestPassport+guestCountry. */
    private static GuestIdentity identity(JsonNode body) {
        String cpf = optionalText(body, "guestCpf");
        String cnpj = optionalText(body, "companyCnpj");
        String company = optionalText(body, "companyName");
        String passport = optionalText(body, "guestPassport");
        String country = optionalText(body, "guestCountry");
        boolean individual = cpf != null;
        boolean corporate = cnpj != null || company != null;
        boolean foreign = passport != null || country != null;
        if (java.util.stream.Stream.of(individual, corporate, foreign).filter(b -> b).count() != 1) {
            throw ApiException.invalid("Informe guestCpf, ou companyCnpj e companyName, ou guestPassport e guestCountry");
        }
        if (individual) return new GuestIdentity.Individual(cpf);
        if (corporate) {
            if (cnpj == null || company == null) throw ApiException.invalid("companyCnpj e companyName vão juntos");
            return new GuestIdentity.Company(cnpj, company);
        }
        if (passport == null || country == null) throw ApiException.invalid("guestPassport e guestCountry vão juntos");
        return new GuestIdentity.Foreigner(passport, country);
    }

    /** Absent, null or blank counts as missing (null); a non-string value is invalid. */
    private static String optionalText(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) return null;
        if (!node.isTextual()) throw ApiException.invalid(field + " deve ser texto");
        return node.asText().isBlank() ? null : node.asText().strip();
    }

    private static String text(JsonNode body, String field) {
        String value = optionalText(body, field);
        if (value == null) throw ApiException.invalid(field + " é obrigatório");
        return value;
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) throw ApiException.invalid(field + " é obrigatório");
        if (!node.isIntegralNumber() || !node.canConvertToInt()) throw ApiException.invalid(field + " deve ser inteiro");
        return node.intValue();
    }

    private static LocalDate parseDate(String raw, String field) {
        if (raw == null || !DATE.matcher(raw).matches()) throw ApiException.invalid(field + " deve ser AAAA-MM-DD");
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve ser AAAA-MM-DD");
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }

    // ---- responses ----

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void error(Context ctx, int status, String code, String message) {
        json(ctx, status, Map.of("error", code, "message", message));
    }

    private static Map<String, Object> roomView(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.type().capacity);
        m.put("nightlyRate", r.type().nightlyRate);
        return m;
    }

    private static void identityFields(Map<String, Object> m, GuestIdentity id) {
        m.put("guestCpf", id.cpf());
        m.put("companyCnpj", id.cnpj());
        m.put("companyName", id.companyName());
        m.put("guestPassport", id.passport());
        m.put("guestCountry", id.country());
    }

    private static Map<String, Object> reservationView(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.roomNumber());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        identityFields(m, r.identity());
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

    private static Map<String, Object> documentView(BookingDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", d.title());
        m.put("reservationId", d.reservationId());
        m.put("guestName", d.guestName());
        identityFields(m, d.identity());
        m.put("roomNumber", d.roomNumber());
        m.put("roomType", d.roomType());
        m.put("checkIn", d.checkIn().toString());
        m.put("checkOut", d.checkOut().toString());
        m.put("nights", d.nights());
        m.put("subtotal", d.subtotal());
        m.put("discount", d.discount());
        m.put("total", d.total());
        m.put("text", d.text());
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
}
