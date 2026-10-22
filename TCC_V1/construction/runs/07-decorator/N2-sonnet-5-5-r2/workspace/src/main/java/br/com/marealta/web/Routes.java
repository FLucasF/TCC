package br.com.marealta.web;

import br.com.marealta.domain.*;
import br.com.marealta.messaging.MessageLog;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.report.OccupancyReport;
import br.com.marealta.report.RevenueReport;
import br.com.marealta.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Routes {
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();
    private final ReservationService service;
    private final ReservationRepository reservations;
    private final MessageLog messages;

    public Routes(ReservationService service, ReservationRepository reservations, MessageLog messages) {
        this.service = service;
        this.reservations = reservations;
        this.messages = messages;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        app.error(404, ctx -> {
            if (ctx.result() == null) error(ctx, 404, "NOT_FOUND", "Recurso não encontrado");
        });

        app.get("/rooms", ctx -> json(ctx, 200, RoomCatalog.all().stream().map(this::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, view(service.create(parseNewReservation(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, service.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, messages.all().stream().map(m -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", m.id());
            v.put("channel", m.channel());
            v.put("to", m.to());
            v.put("subject", m.subject());
            v.put("body", m.body());
            v.put("status", m.status());
            return v;
        }).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                new OccupancyReport(reservations, parseDate(ctx.queryParam("date"), "date")).render()));
        app.get("/reports/revenue", ctx -> csv(ctx,
                new RevenueReport(reservations, parseMonth(ctx.queryParam("month"))).render()));
        return app.start(port);
    }

    private Map<String, Object> roomView(Room r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("number", r.number());
        v.put("type", r.type());
        v.put("capacity", r.capacity());
        v.put("nightlyRate", r.nightlyRate());
        return v;
    }

    private Map<String, Object> view(Reservation r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", r.id());
        v.put("roomNumber", r.roomNumber());
        v.put("guestName", r.guestName());
        v.put("guestEmail", r.guestEmail());
        v.put("guestCpf", r.guestCpf());
        v.put("checkIn", r.checkInDate());
        v.put("checkOut", r.checkOutDate());
        v.put("guests", r.guests());
        v.put("ratePlan", r.ratePlan());
        v.put("nights", r.nights());
        v.put("subtotal", r.subtotal());
        v.put("discount", r.discount());
        v.put("total", r.total());
        v.put("status", r.status());
        v.put("amountPaid", r.amountPaid());
        v.put("refund", r.refund());
        return v;
    }

    private ReservationService.NewReservation parseNewReservation(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw ApiException.invalidRequest("O corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalidRequest("O corpo deve ser um objeto JSON");

        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = parseDate(text(body, "checkIn"), "checkIn");
        LocalDate checkOut = parseDate(text(body, "checkOut"), "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidRequest("ratePlan inválido");
        }
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalidRequest("A saída deve ser depois da entrada");
        if (guests < 1) throw ApiException.invalidRequest("É preciso pelo menos um hóspede");
        return new ReservationService.NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw ApiException.invalidRequest("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asInt();
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            if (value == null) throw new DateTimeParseException("ausente", "", 0);
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Data inválida em " + field + " (use AAAA-MM-DD)");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            if (value == null) throw new DateTimeParseException("ausente", "", 0);
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.invalidRequest("Mês inválido (use AAAA-MM)");
        }
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(0);
        }
    }

    private void json(Context ctx, int status, Object body) throws JsonProcessingException {
        ctx.status(status).contentType("application/json; charset=utf-8").result(mapper.writeValueAsString(body));
    }

    private void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private void error(Context ctx, int status, String code, String message) {
        try {
            json(ctx, status, Map.of("error", code, "message", message));
        } catch (JsonProcessingException e) {
            ctx.status(status);
        }
    }
}
