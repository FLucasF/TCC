package br.com.marealta;

import br.com.marealta.Model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Api {
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private final ReservationService service;

    private Api(ReservationService service) {
        this.service = service;
    }

    public static Javalin create(ReservationService service) {
        Api api = new Api(service);
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);
        app.get("/rooms", api::rooms);
        app.post("/reservations", api::create);
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservation(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservation(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservation(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, document(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, document(service.document(id(ctx)))));
        app.get("/messages", api::messages);
        app.get("/reports/occupancy", ctx -> csv(ctx, service.occupancyCsv(date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, service.revenueCsv(month(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    // ---- handlers

    private void rooms(Context ctx) {
        List<Map<String, Object>> out = service.rooms().stream().map(r -> obj(
                "number", r.number(), "type", r.type(), "capacity", r.capacity(), "nightlyRate", r.nightlyRate())).toList();
        json(ctx, 200, out);
    }

    private void messages(Context ctx) {
        List<Map<String, Object>> out = service.messages().stream().map(m -> obj(
                "id", m.id(), "channel", m.channel(), "to", m.to(), "subject", m.subject(), "body", m.body())).toList();
        json(ctx, 200, out);
    }

    private void create(Context ctx) {
        json(ctx, 201, reservation(service.create(parse(ctx.body()))));
    }

    // ---- entrada

    static ReservationService.NewReservation parse(String body) {
        JsonNode n;
        try {
            n = JSON.readTree(body);
        } catch (JsonProcessingException e) {
            throw ApiException.invalid("O corpo não é um JSON válido");
        }
        if (n == null || !n.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");

        String name = text(n, "guestName");
        String email = text(n, "guestEmail");
        if (name == null) throw ApiException.invalid("guestName é obrigatório");
        if (email == null) throw ApiException.invalid("guestEmail é obrigatório");
        Identification ident = identification(n);
        LocalDate checkIn = date(text(n, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(n, "checkOut"), "checkOut");
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída deve ser depois da entrada");

        JsonNode g = n.get("guests");
        if (g == null || !g.isIntegralNumber() || !g.canConvertToInt()) {
            throw ApiException.invalid("guests deve ser um número inteiro");
        }
        if (g.intValue() < 1) throw ApiException.invalid("É preciso ao menos um hóspede");

        JsonNode rn = n.get("roomNumber");
        if (rn == null || !rn.isIntegralNumber() || !rn.canConvertToInt()) {
            throw ApiException.invalid("roomNumber deve ser um número inteiro");
        }
        String plan = text(n, "ratePlan");
        if (plan == null) throw ApiException.invalid("ratePlan é obrigatório");
        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf(plan);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        return new ReservationService.NewReservation(rn.intValue(), name, email, ident, checkIn, checkOut,
                g.intValue(), ratePlan);
    }

    /** Exatamente um dos três jeitos: CPF, CNPJ + razão social, ou passaporte + país. */
    private static Identification identification(JsonNode n) {
        for (String f : List.of("guestCpf", "companyCnpj", "companyName", "guestPassport", "guestCountry")) {
            JsonNode v = n.get(f);
            if (v != null && !v.isNull() && !v.isTextual()) throw ApiException.invalid(f + " deve ser texto");
        }
        String cpf = text(n, "guestCpf");
        String cnpj = text(n, "companyCnpj");
        String company = text(n, "companyName");
        String passport = text(n, "guestPassport");
        String country = text(n, "guestCountry");
        boolean isCpf = cpf != null;
        boolean isCompany = cnpj != null || company != null;
        boolean isPassport = passport != null || country != null;
        if (java.util.stream.Stream.of(isCpf, isCompany, isPassport).filter(b -> b).count() != 1) {
            throw ApiException.invalid("Informe guestCpf, ou companyCnpj com companyName, ou guestPassport com guestCountry");
        }
        if (isCpf) return new Identification.Cpf(cpf);
        if (isCompany) {
            if (cnpj == null || company == null) throw ApiException.invalid("companyCnpj e companyName vão juntos");
            return new Identification.Company(cnpj, company);
        }
        if (passport == null || country == null) throw ApiException.invalid("guestPassport e guestCountry vão juntos");
        return new Identification.Passport(passport, country);
    }

    /** Texto não vazio, ou null se ausente/nulo/vazio. */
    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        if (!v.isTextual()) throw ApiException.invalid(field + " deve ser texto");
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private static LocalDate date(String v, String field) {
        if (v == null) throw ApiException.invalid(field + " é obrigatório (formato AAAA-MM-DD)");
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " deve estar no formato AAAA-MM-DD");
        }
    }

    private static YearMonth month(String v) {
        if (v == null) throw ApiException.invalid("month é obrigatório (formato AAAA-MM)");
        try {
            return YearMonth.parse(v);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("month deve estar no formato AAAA-MM");
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }

    // ---- saída

    private static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    /** Os cinco campos de identificação; os que não se aplicam vão null. */
    private static void putIdentification(Map<String, Object> m, Identification id) {
        m.put("guestCpf", id instanceof Identification.Cpf c ? c.cpf() : null);
        m.put("companyCnpj", id instanceof Identification.Company c ? c.cnpj() : null);
        m.put("companyName", id instanceof Identification.Company c ? c.name() : null);
        m.put("guestPassport", id instanceof Identification.Passport p ? p.number() : null);
        m.put("guestCountry", id instanceof Identification.Passport p ? p.country() : null);
    }

    static Map<String, Object> reservation(Reservation r) {
        Map<String, Object> m = obj("id", r.id(), "roomNumber", r.roomNumber(), "guestName", r.guestName(),
                "guestEmail", r.guestEmail());
        putIdentification(m, r.identification());
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

    static Map<String, Object> document(ReservationService.Issued i) {
        Reservation r = i.reservation();
        Map<String, Object> m = obj("title", i.document().title(), "reservationId", r.id(), "guestName", r.guestName());
        putIdentification(m, r.identification());
        m.put("roomNumber", r.roomNumber());
        m.put("roomType", i.room().type());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("text", i.document().text());
        return m;
    }

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
        json(ctx, status, obj("error", code, "message", message));
    }
}
