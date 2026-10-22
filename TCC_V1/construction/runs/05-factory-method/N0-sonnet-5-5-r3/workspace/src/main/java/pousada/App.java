package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import pousada.Domain.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final String CSV = "text/csv; charset=utf-8";

    private final Service service;

    public App(Service service) {
        this.service = service;
    }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.jsonMapper(new JavalinJackson(MAPPER, false));
        });

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            String type = ctx.res().getContentType();
            if (type == null || !type.startsWith("application/json")) {
                error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
            }
        });

        app.get("/rooms", ctx -> ctx.json(service.rooms().stream().map(App::roomJson).toList()));

        app.post("/reservations", ctx -> ctx.status(201).json(reservationJson(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> ctx.json(reservationJson(service.reservation(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(reservationJson(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(reservationJson(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(reservationJson(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(documentJson(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(documentJson(service.document(id(ctx)))));

        app.get("/messages", ctx -> ctx.json(service.messages().stream().map(App::messageJson).toList()));

        app.get("/reports/occupancy", ctx -> {
            String date = ctx.queryParam("date");
            if (date == null || !DATE.matcher(date).matches()) throw ApiException.invalid("Parâmetro date inválido (use AAAA-MM-DD)");
            LocalDate d;
            try {
                d = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro date inválido (use AAAA-MM-DD)");
            }
            ctx.contentType(CSV).result(service.occupancyCsv(d));
        });
        app.get("/reports/revenue", ctx -> {
            String month = ctx.queryParam("month");
            if (month == null || !MONTH.matcher(month).matches()) throw ApiException.invalid("Parâmetro month inválido (use AAAA-MM)");
            YearMonth m;
            try {
                m = YearMonth.parse(month);
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro month inválido (use AAAA-MM)");
            }
            ctx.contentType(CSV).result(service.revenueCsv(m));
        });
        return app;
    }

    // ---- entrada ----

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada: " + ctx.pathParam("id"));
        }
    }

    private static Service.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (Exception e) {
            throw ApiException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("Corpo da requisição deve ser um objeto JSON");

        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName", true);
        String email = text(body, "guestEmail", true);
        if (!email.contains("@")) throw ApiException.invalid("guestEmail inválido");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("checkOut deve ser depois de checkIn");
        int guests = integer(body, "guests");
        if (guests < 1) throw ApiException.invalid("guests deve ser de pelo menos 1");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan", true));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido (STANDARD ou NON_REFUNDABLE)");
        }

        String cpf = text(body, "guestCpf", false);
        String cnpj = text(body, "companyCnpj", false);
        String company = text(body, "companyName", false);
        String passport = text(body, "guestPassport", false);
        String country = text(body, "guestCountry", false);
        boolean individual = cpf != null && cnpj == null && company == null && passport == null && country == null;
        boolean corporate = cnpj != null && company != null && cpf == null && passport == null && country == null;
        boolean foreign = passport != null && country != null && cpf == null && cnpj == null && company == null;
        if (!(individual || corporate || foreign)) {
            throw ApiException.invalid("Informe guestCpf, ou companyCnpj com companyName, ou guestPassport com guestCountry");
        }
        return new Service.NewReservation(roomNumber, name, email,
                Identification.from(cpf, cnpj, company, passport, country), checkIn, checkOut, guests, plan);
    }

    /** Texto opcional ou obrigatório; vazio conta como ausente. */
    private static String text(JsonNode body, String field, boolean required) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) {
            if (required) throw ApiException.invalid("Campo obrigatório: " + field);
            return null;
        }
        if (!n.isTextual()) throw ApiException.invalid("Campo " + field + " deve ser texto");
        String s = n.asText().trim();
        if (s.isEmpty()) {
            if (required) throw ApiException.invalid("Campo obrigatório: " + field);
            return null;
        }
        return s;
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) throw ApiException.invalid("Campo obrigatório: " + field);
        if (!n.isIntegralNumber() || !n.canConvertToInt()) throw ApiException.invalid("Campo " + field + " deve ser um número inteiro");
        return n.intValue();
    }

    private static LocalDate date(JsonNode body, String field) {
        String s = text(body, field, true);
        if (!DATE.matcher(s).matches()) throw ApiException.invalid("Campo " + field + " deve estar no formato AAAA-MM-DD");
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Campo " + field + " é uma data inválida");
        }
    }

    // ---- saída ----

    private static void error(Context ctx, int status, String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", code);
        m.put("message", message);
        ctx.status(status).json(m);
    }

    private static Map<String, Object> roomJson(Room r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", r.number());
        m.put("type", r.type());
        m.put("capacity", r.capacity());
        m.put("nightlyRate", r.nightlyRate());
        return m;
    }

    private static void identification(Map<String, Object> m, Identification i) {
        m.put("guestCpf", i.cpf());
        m.put("companyCnpj", i.cnpj());
        m.put("companyName", i.companyName());
        m.put("guestPassport", i.passport());
        m.put("guestCountry", i.country());
    }

    private static Map<String, Object> reservationJson(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id());
        m.put("roomNumber", r.roomNumber());
        m.put("guestName", r.guestName());
        m.put("guestEmail", r.guestEmail());
        identification(m, r.identification());
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

    private static Map<String, Object> documentJson(Service.Document d) {
        Identification.Receipt r = d.receipt();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", d.reservation().identification().title());
        m.put("reservationId", r.reservationId());
        m.put("guestName", r.guestName());
        identification(m, d.reservation().identification());
        m.put("roomNumber", r.roomNumber());
        m.put("roomType", r.roomType());
        m.put("checkIn", r.checkIn().toString());
        m.put("checkOut", r.checkOut().toString());
        m.put("nights", r.nights());
        m.put("subtotal", r.subtotal());
        m.put("discount", r.discount());
        m.put("total", r.total());
        m.put("text", d.text());
        return m;
    }

    private static Map<String, Object> messageJson(Message m) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", m.id());
        o.put("channel", m.channel());
        o.put("to", m.to());
        o.put("subject", m.subject());
        o.put("body", m.body());
        return o;
    }
}
