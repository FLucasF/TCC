package com.marealta.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marealta.domain.ErrorCode;
import com.marealta.domain.Money;
import com.marealta.domain.PousadaException;
import com.marealta.domain.RatePlan;
import com.marealta.domain.Receipt;
import com.marealta.domain.Reservation;
import com.marealta.domain.Room;
import com.marealta.domain.RoomCatalog;
import com.marealta.persistence.MessageRepository;
import com.marealta.service.NewReservation;
import com.marealta.service.ReportService;
import com.marealta.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class Routes {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageRepository messages;
    private final RoomCatalog rooms;

    public Routes(ReservationService reservations, ReportService reports, MessageRepository messages,
                  RoomCatalog rooms) {
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
        this.rooms = rooms;
    }

    public void register(Javalin app) {
        app.exception(PousadaException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> json(ctx, 200, rooms.all().stream().map(this::roomView).toList()));
        app.post("/reservations", ctx -> json(ctx, 201, view(reservations.create(parseNewReservation(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, view(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, view(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, view(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, view(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, receiptView(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, receiptView(reservations.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, messages.findAll().stream().map(m -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("id", m.id());
            v.put("channel", m.channel());
            v.put("to", m.to());
            v.put("subject", m.subject());
            v.put("body", m.body());
            return v;
        }).toList()));
        app.get("/reports/occupancy", ctx ->
                csv(ctx, reports.occupancy(queryParam(ctx, "date", LocalDate::parse))));
        app.get("/reports/revenue", ctx ->
                csv(ctx, reports.revenue(queryParam(ctx, "month", YearMonth::parse))));
    }

    private NewReservation parseNewReservation(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        int roomNumber = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int guests = integer(body, "guests");
        RatePlan plan = ratePlan(body);
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso ao menos um hóspede");
        }
        return new NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw invalid("Campo '" + field + "' ausente ou inválido");
        }
        return n.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isInt()) {
            throw invalid("Campo '" + field + "' ausente ou inválido");
        }
        return n.asInt();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw invalid("Campo '" + field + "' deve estar no formato AAAA-MM-DD");
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        try {
            return RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Campo 'ratePlan' deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    private static <T> T queryParam(Context ctx, String name, Function<String, T> parser) {
        String value = ctx.queryParam(name);
        if (value == null) {
            throw invalid("Parâmetro '" + name + "' ausente");
        }
        try {
            return parser.apply(value);
        } catch (DateTimeParseException e) {
            throw invalid("Parâmetro '" + name + "' em formato inválido");
        }
    }

    private static long id(Context ctx) {
        String raw = ctx.pathParam("id");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + raw + " não existe");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }

    private Map<String, Object> roomView(Room room) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("number", room.number());
        v.put("type", room.type());
        v.put("capacity", room.capacity());
        v.put("nightlyRate", room.nightlyRate());
        return v;
    }

    private Map<String, Object> view(Reservation r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", r.id());
        v.put("roomNumber", r.roomNumber());
        v.put("guestName", r.guestName());
        v.put("guestEmail", r.guestEmail());
        v.put("guestCpf", r.guestCpf());
        v.put("checkIn", r.checkIn().toString());
        v.put("checkOut", r.checkOut().toString());
        v.put("guests", r.guests());
        v.put("ratePlan", r.ratePlan());
        v.put("nights", r.quote().nights());
        v.put("subtotal", r.quote().subtotal());
        v.put("discount", r.quote().discount());
        v.put("total", r.quote().total());
        v.put("status", r.status());
        v.put("amountPaid", r.amountPaid());
        v.put("refund", r.refund());
        return v;
    }

    private Map<String, Object> receiptView(Receipt c) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("title", c.title());
        v.put("reservationId", c.reservationId());
        v.put("guestName", c.guestName());
        v.put("guestCpf", c.guestCpf());
        v.put("roomNumber", c.roomNumber());
        v.put("roomType", c.roomType());
        v.put("checkIn", c.checkIn().toString());
        v.put("checkOut", c.checkOut().toString());
        v.put("nights", c.nights());
        v.put("subtotal", c.subtotal());
        v.put("discount", c.discount());
        v.put("total", c.total());
        return v;
    }

    private void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("error", code);
        v.put("message", message);
        json(ctx, status, v);
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }
}
