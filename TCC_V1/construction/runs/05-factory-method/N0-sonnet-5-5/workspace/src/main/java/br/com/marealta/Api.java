package br.com.marealta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;

import java.util.Map;

final class Api {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Service service;

    Api(Service service) { this.service = service; }

    Javalin create() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().stream().map(Api::roomJson).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, reservationJson(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservationJson(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservationJson(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservationJson(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservationJson(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, documentJson(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, documentJson(service.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, service.messages().stream().map(Api::messageJson).toList()));

        app.get("/reports/occupancy", ctx -> {
            String raw = ctx.queryParam("date");
            if (raw == null) throw ApiException.invalid("Parâmetro date é obrigatório (AAAA-MM-DD)");
            csv(ctx, service.occupancyReport(parseDate(raw, "date")));
        });
        app.get("/reports/revenue", ctx -> {
            String raw = ctx.queryParam("month");
            if (raw == null) throw ApiException.invalid("Parâmetro month é obrigatório (AAAA-MM)");
            YearMonth month;
            try {
                month = YearMonth.parse(raw);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("month inválido, use AAAA-MM");
            }
            csv(ctx, service.revenueReport(month));
        });
        return app;
    }

    // ---------- entrada ----------

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }

    private static Service.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.bodyAsBytes());
        } catch (IOException e) {
            throw ApiException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("Corpo da requisição deve ser um objeto JSON");

        int roomNumber = requiredInt(body, "roomNumber");
        String name = requiredText(body, "guestName");
        String email = requiredText(body, "guestEmail");
        LocalDate checkIn = parseDate(requiredText(body, "checkIn"), "checkIn");
        LocalDate checkOut = parseDate(requiredText(body, "checkOut"), "checkOut");
        int guests = requiredInt(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(requiredText(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido: use STANDARD ou NON_REFUNDABLE");
        }
        Identification identification = parseIdentification(body);
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("checkOut deve ser depois de checkIn");
        if (guests < 1) throw ApiException.invalid("guests deve ser pelo menos 1");
        return new Service.NewReservation(roomNumber, name, email, identification, checkIn, checkOut, guests, plan);
    }

    /** Exatamente um: CPF; CNPJ + razão social; ou passaporte + país. */
    private static Identification parseIdentification(JsonNode body) {
        String cpf = optionalText(body, "guestCpf");
        String cnpj = optionalText(body, "companyCnpj");
        String company = optionalText(body, "companyName");
        String passport = optionalText(body, "guestPassport");
        String country = optionalText(body, "guestCountry");

        boolean isCpf = cpf != null;
        boolean isCompany = cnpj != null || company != null;
        boolean isPassport = passport != null || country != null;
        if ((isCpf ? 1 : 0) + (isCompany ? 1 : 0) + (isPassport ? 1 : 0) != 1) {
            throw ApiException.invalid("Informe guestCpf, ou companyCnpj com companyName, ou guestPassport com guestCountry");
        }
        if (isCpf) return new Identification.Cpf(cpf);
        if (isCompany) {
            if (cnpj == null || company == null) throw ApiException.invalid("companyCnpj e companyName devem vir juntos");
            return new Identification.Company(cnpj, company);
        }
        if (passport == null || country == null) throw ApiException.invalid("guestPassport e guestCountry devem vir juntos");
        return new Identification.Passport(passport, country);
    }

    private static String requiredText(JsonNode body, String field) {
        String v = optionalText(body, field);
        if (v == null) throw ApiException.invalid("Campo obrigatório ausente ou vazio: " + field);
        return v;
    }

    /** Ausente, null ou em branco = null; outro tipo que não texto é inválido. */
    private static String optionalText(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) return null;
        if (!n.isTextual()) throw ApiException.invalid("Campo " + field + " deve ser texto");
        return n.asText().isBlank() ? null : n.asText();
    }

    private static int requiredInt(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) throw ApiException.invalid("Campo obrigatório ausente: " + field);
        if (!n.isIntegralNumber() || !n.canConvertToInt()) throw ApiException.invalid("Campo " + field + " deve ser um inteiro");
        return n.intValue();
    }

    private static LocalDate parseDate(String raw, String field) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid(field + " inválida, use AAAA-MM-DD");
        }
    }

    // ---------- saída ----------

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
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        json(ctx, status, m);
    }

    private static Map<String, Object> roomJson(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
        return m;
    }

    private static Map<String, Object> messageJson(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", msg.id());
        m.put("channel", msg.channel());
        m.put("to", msg.to());
        m.put("subject", msg.subject());
        m.put("body", msg.body());
        return m;
    }

    /** Os cinco campos de identificação; os que não se aplicam vão null. */
    private static void putIdentification(Map<String, Object> m, Identification id) {
        m.put("guestCpf", id instanceof Identification.Cpf c ? c.cpf() : null);
        m.put("companyCnpj", id instanceof Identification.Company c ? c.cnpj() : null);
        m.put("companyName", id instanceof Identification.Company c ? c.companyName() : null);
        m.put("guestPassport", id instanceof Identification.Passport p ? p.passport() : null);
        m.put("guestCountry", id instanceof Identification.Passport p ? p.country() : null);
    }

    private static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.roomNumber());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
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

    private static Map<String, Object> documentJson(Service.IssuedDocument d) {
        Reservation r = d.reservation();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", d.document().title());
        m.put("reservationId", r.id());
        m.put("guestName", r.guestName());
        putIdentification(m, r.identification());
        m.put("roomNumber", d.room().number());
        m.put("roomType", d.room().type());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("text", d.document().text());
        return m;
    }
}
