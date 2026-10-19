package br.com.marealta;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Api {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private final ReservationService service;
    private final Outbox outbox;

    Api(ReservationService service, Outbox outbox) {
        this.service = service;
        this.outbox = outbox;
    }

    Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", "Erro interno"));
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
            }
        });

        app.get("/rooms", ctx -> json(ctx, 200, Room.ALL.stream().map(Api::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationView(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationView(service.get(ctx.pathParam("id")))));
        app.post("/reservations/{id}/confirmation",
                ctx -> json(ctx, 200, reservationView(service.confirm(ctx.pathParam("id")))));
        app.post("/reservations/{id}/cancellation",
                ctx -> json(ctx, 200, reservationView(service.cancel(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-in",
                ctx -> json(ctx, 200, reservationView(service.checkIn(ctx.pathParam("id")))));
        app.post("/reservations/{id}/check-out",
                ctx -> json(ctx, 200, documentView(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document",
                ctx -> json(ctx, 200, documentView(service.duplicate(ctx.pathParam("id")))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.all().stream().map(Api::messageView).toList()));
        app.get("/reports/occupancy", ctx -> {
            LocalDate date = date(ctx.queryParam("date"), "date");
            csv(ctx, Reports.occupancy(service.all(), date));
        });
        app.get("/reports/revenue", ctx -> {
            String m = ctx.queryParam("month");
            YearMonth month;
            try {
                if (m == null) throw new DateTimeParseException("missing", "", 0);
                month = YearMonth.parse(m);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("month deve estar no formato AAAA-MM");
            }
            csv(ctx, Reports.revenue(service.all(), month));
        });
        return app;
    }

    // ---- request parsing ----

    private ReservationService.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.bodyAsBytes());
        } catch (java.io.IOException e) {
            throw ApiException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw ApiException.invalid("Corpo da requisição deve ser um objeto JSON");
        }
        String name = requiredText(body, "guestName");
        String email = requiredText(body, "guestEmail");
        LocalDate checkIn = date(optionalText(body, "checkIn"), "checkIn");
        LocalDate checkOut = date(optionalText(body, "checkOut"), "checkOut");
        JsonNode roomNode = body.get("roomNumber");
        if (roomNode == null || !roomNode.isIntegralNumber() || !roomNode.canConvertToInt()) {
            throw ApiException.invalid("roomNumber é obrigatório e deve ser inteiro");
        }
        JsonNode guestsNode = body.get("guests");
        if (guestsNode == null || !guestsNode.isIntegralNumber() || !guestsNode.canConvertToInt()) {
            throw ApiException.invalid("guests é obrigatório e deve ser inteiro");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(requiredText(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.invalid("checkOut deve ser depois de checkIn");
        }
        if (guestsNode.intValue() < 1) {
            throw ApiException.invalid("guests deve ser pelo menos 1");
        }
        return new ReservationService.NewReservation(roomNode.intValue(), name, email, identification(body),
                checkIn, checkOut, guestsNode.intValue(), plan);
    }

    private static Identification identification(JsonNode body) {
        String cpf = optionalText(body, "guestCpf");
        String cnpj = optionalText(body, "companyCnpj");
        String company = optionalText(body, "companyName");
        String passport = optionalText(body, "guestPassport");
        String country = optionalText(body, "guestCountry");
        boolean isCpf = cpf != null;
        boolean isCnpj = cnpj != null && company != null;
        boolean isPassport = passport != null && country != null;
        int provided = (cpf != null ? 1 : 0) + (cnpj != null ? 1 : 0) + (company != null ? 1 : 0)
                + (passport != null ? 1 : 0) + (country != null ? 1 : 0);
        int expected = isCpf ? 1 : isCnpj ? 2 : isPassport ? 2 : -1;
        boolean single = (isCpf ? 1 : 0) + (isCnpj ? 1 : 0) + (isPassport ? 1 : 0) == 1;
        if (!single || provided != expected) {
            throw ApiException.invalid("Informe exatamente um: guestCpf; companyCnpj com companyName; "
                    + "ou guestPassport com guestCountry");
        }
        if (isCpf) return new Identification.Cpf(cpf);
        if (isCnpj) return new Identification.Cnpj(cnpj, company);
        return new Identification.Passport(passport, country);
    }

    /** Null when missing, null or blank; 400 when present but not a string. */
    private static String optionalText(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) return null;
        if (!n.isTextual()) throw ApiException.invalid(field + " deve ser texto");
        String s = n.textValue().strip();
        return s.isEmpty() ? null : s;
    }

    private static String requiredText(JsonNode body, String field) {
        String s = optionalText(body, field);
        if (s == null) throw ApiException.invalid(field + " é obrigatório");
        return s;
    }

    private static LocalDate date(String value, String field) {
        if (value == null) throw ApiException.invalid(field + " é obrigatório (formato AAAA-MM-DD)");
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    // ---- views ----

    private static Map<String, Object> roomView(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
        return m;
    }

    private static void putIdentification(Map<String, Object> m, Identification id) {
        m.put("guestCpf", id instanceof Identification.Cpf c ? c.cpf() : null);
        m.put("companyCnpj", id instanceof Identification.Cnpj c ? c.cnpj() : null);
        m.put("companyName", id instanceof Identification.Cnpj c ? c.companyName() : null);
        m.put("guestPassport", id instanceof Identification.Passport p ? p.passport() : null);
        m.put("guestCountry", id instanceof Identification.Passport p ? p.country() : null);
    }

    private static Map<String, Object> reservationView(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id);
        m.put("roomNumber", r.room.number());
        m.put("guestName", r.guestName);
        m.put("guestEmail", r.guestEmail);
        putIdentification(m, r.identification);
        m.put("checkIn", r.checkIn.toString());
        m.put("checkOut", r.checkOut.toString());
        m.put("guests", r.guests);
        m.put("ratePlan", r.ratePlan);
        m.put("nights", r.quote.nights());
        m.put("subtotal", r.quote.subtotal());
        m.put("discount", r.quote.discount());
        m.put("total", r.quote.total());
        m.put("status", r.status);
        m.put("amountPaid", r.amountPaid);
        m.put("refund", r.refund);
        return m;
    }

    private static Map<String, Object> documentView(Document d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", d.title());
        m.put("reservationId", d.reservationId());
        m.put("guestName", d.guestName());
        putIdentification(m, d.identification());
        m.put("roomNumber", d.room().number());
        m.put("roomType", d.room().type());
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

    // ---- responses ----

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
}
