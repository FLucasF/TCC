package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.regex.Pattern;

public class Api {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final Reports reports;

    public Api(ReservationService service, Reports reports) {
        this.service = service;
        this.reports = reports;
    }

    record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
            String companyCnpj, String companyName, String guestPassport, String guestCountry, String checkIn,
            String checkOut, int guests, RatePlan ratePlan, int nights, BigDecimal subtotal, BigDecimal discount,
            BigDecimal total, Status status, BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            Identification i = r.identification();
            return new ReservationView(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), i.cpf(), i.cnpj(),
                    i.companyName(), i.passport(), i.country(), r.checkIn().toString(), r.checkOut().toString(),
                    r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(), r.status(),
                    r.amountPaid(), r.refund());
        }
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);
        app.get("/rooms", ctx -> ctx.json(Room.ALL.stream()
                .map(r -> new RoomView(r.number(), r.type(), r.capacity(), r.type().nightlyRate)).toList()));
        app.post("/reservations", ctx -> ctx.status(201).json(ReservationView.of(service.create(parse(ctx)))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> {
            long id = id(ctx);
            service.checkOut(id);
            ctx.json(service.document(id));
        });
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(service.messages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(reports.occupancy(
                date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(reports.revenue(month(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.status).json(Map.of("error", e.code, "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app.start(port);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }

    private static ReservationService.NewReservation parse(Context ctx) {
        JsonNode b;
        try {
            b = JSON.readTree(ctx.body());
        } catch (Exception e) {
            throw ApiException.invalid("Corpo da requisição não é um JSON válido");
        }
        if (b == null || !b.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");

        String name = required(b, "guestName");
        String email = required(b, "guestEmail");
        JsonNode room = b.get("roomNumber");
        if (room == null || !room.isIntegralNumber() || !room.canConvertToInt()) {
            throw ApiException.invalid("roomNumber inválido");
        }
        LocalDate in = date(required(b, "checkIn"), "checkIn");
        LocalDate out = date(required(b, "checkOut"), "checkOut");
        if (!out.isAfter(in)) throw ApiException.invalid("A saída deve ser depois da entrada");
        JsonNode g = b.get("guests");
        if (g == null || !g.isIntegralNumber() || !g.canConvertToInt() || g.asInt() < 1) {
            throw ApiException.invalid("É preciso pelo menos um hóspede");
        }
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(required(b, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido");
        }
        return new ReservationService.NewReservation(room.asInt(), name, email, identification(b), in, out,
                g.asInt(), plan);
    }

    private static Identification identification(JsonNode b) {
        String cpf = optional(b, "guestCpf");
        String cnpj = optional(b, "companyCnpj");
        String company = optional(b, "companyName");
        String passport = optional(b, "guestPassport");
        String country = optional(b, "guestCountry");
        boolean isCpf = cpf != null;
        boolean isCompany = cnpj != null || company != null;
        boolean isForeign = passport != null || country != null;
        if ((isCpf ? 1 : 0) + (isCompany ? 1 : 0) + (isForeign ? 1 : 0) != 1) {
            throw ApiException.invalid("Informe só uma identificação: CPF, CNPJ com razão social, ou passaporte com país");
        }
        if (isCpf) return new Identification.Cpf(cpf);
        if (isCompany) {
            if (cnpj == null || company == null) throw ApiException.invalid("Informe companyCnpj e companyName");
            return new Identification.Cnpj(cnpj, company);
        }
        if (passport == null || country == null) throw ApiException.invalid("Informe guestPassport e guestCountry");
        return new Identification.Passport(passport, country);
    }

    private static String optional(JsonNode b, String field) {
        JsonNode n = b.get(field);
        if (n == null || n.isNull()) return null;
        if (!n.isTextual()) throw ApiException.invalid(field + " deve ser texto");
        return n.asText().isBlank() ? null : n.asText();
    }

    private static String required(JsonNode b, String field) {
        String v = optional(b, field);
        if (v == null) throw ApiException.invalid("Campo obrigatório ausente: " + field);
        return v;
    }

    private static LocalDate date(String v, String field) {
        if (v == null || !DATE.matcher(v).matches()) throw ApiException.invalid("Data inválida em " + field);
        try {
            return LocalDate.parse(v);
        } catch (Exception e) {
            throw ApiException.invalid("Data inválida em " + field);
        }
    }

    private static YearMonth month(String v) {
        if (v == null || !MONTH.matcher(v).matches()) throw ApiException.invalid("Mês inválido");
        try {
            return YearMonth.parse(v);
        } catch (Exception e) {
            throw ApiException.invalid("Mês inválido");
        }
    }
}
