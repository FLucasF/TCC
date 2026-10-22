package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.ApiException;
import br.com.marealta.pousada.Domain.RatePlan;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.regex.Pattern;

public class Api {
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService service;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public Api(ReservationService service) {
        this.service = service;
    }

    public Javalin build() {
        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.jsonMapper(new JavalinJackson(mapper, false));
        });
        app.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.status).json(Map.of("error", e.code, "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(service.rooms()));
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(parse(ctx))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(service.messages()));
        app.get("/reports/occupancy", ctx -> {
            String raw = ctx.queryParam("date");
            if (raw == null || !DATE.matcher(raw).matches()) throw invalid("Parâmetro date inválido (use AAAA-MM-DD)");
            ctx.contentType(CSV).result(service.occupancyReport(date(raw, "date")));
        });
        app.get("/reports/revenue", ctx -> {
            String raw = ctx.queryParam("month");
            if (raw == null || !MONTH.matcher(raw).matches()) throw invalid("Parâmetro month inválido (use AAAA-MM)");
            YearMonth m;
            try {
                m = YearMonth.parse(raw);
            } catch (RuntimeException e) {
                throw invalid("Parâmetro month inválido (use AAAA-MM)");
            }
            ctx.contentType(CSV).result(service.revenueReport(m));
        });
        return app;
    }

    private static ApiException invalid(String msg) {
        return new ApiException(400, "INVALID_REQUEST", msg);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não encontrada");
        }
    }

    private static LocalDate date(String raw, String field) {
        if (raw == null || !DATE.matcher(raw).matches()) throw invalid("Campo " + field + " inválido (use AAAA-MM-DD)");
        try {
            return LocalDate.parse(raw);
        } catch (RuntimeException e) {
            throw invalid("Campo " + field + " inválido (use AAAA-MM-DD)");
        }
    }

    private ReservationService.NewReservation parse(Context ctx) {
        JsonNode root;
        try {
            root = mapper.readTree(ctx.body());
        } catch (Exception e) {
            throw invalid("Corpo da requisição não é um JSON válido");
        }
        if (root == null || !root.isObject()) throw invalid("Corpo da requisição deve ser um objeto JSON");

        int room = integer(root, "roomNumber");
        String name = text(root, "guestName");
        String email = text(root, "guestEmail");
        String cpf = text(root, "guestCpf");
        LocalDate in = date(text(root, "checkIn"), "checkIn");
        LocalDate out = date(text(root, "checkOut"), "checkOut");
        int guests = integer(root, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(root, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo ratePlan inválido (STANDARD ou NON_REFUNDABLE)");
        }
        if (!out.isAfter(in)) throw invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw invalid("É preciso pelo menos um hóspede");
        return new ReservationService.NewReservation(room, name, email, cpf, in, out, guests, plan);
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) throw invalid("Campo " + field + " ausente ou inválido");
        return n.asText().trim();
    }

    private static int integer(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw invalid("Campo " + field + " ausente ou inválido");
        }
        return n.asInt();
    }
}
