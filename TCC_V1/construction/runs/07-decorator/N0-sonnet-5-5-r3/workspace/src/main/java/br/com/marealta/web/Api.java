package br.com.marealta.web;

import br.com.marealta.Db;
import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reports;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationService;
import br.com.marealta.domain.Room;
import br.com.marealta.mail.Mailer;
import br.com.marealta.mail.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

/** Rotas HTTP combinadas com o site. */
public final class Api {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private final Db db;
    private final ReservationService service;
    private final Reports reports;

    public Api(Db db, ReservationService service, Reports reports) {
        this.db = db;
        this.service = service;
        this.reports = reports;
    }

    public Javalin build() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status, e.code, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });

        app.get("/rooms", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Room r : service.rooms()) {
                arr.addObject().put("number", r.number()).put("type", r.type()).put("capacity", r.capacity())
                   .put("nightlyRate", r.nightlyRate());
            }
            json(ctx, 200, arr);
        });

        app.post("/reservations", ctx -> json(ctx, 201, reservation(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservation(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservation(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservation(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservation(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, receipt(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, receipt(service.document(id(ctx)))));

        app.get("/messages", ctx -> {
            List<Message> messages = db.tx(Mailer::list);
            ArrayNode arr = JSON.createArrayNode();
            for (Message m : messages) {
                arr.addObject().put("id", m.id()).put("channel", m.channel()).put("to", m.to())
                   .put("subject", m.subject()).put("body", m.body()).put("status", m.status());
            }
            json(ctx, 200, arr);
        });

        app.get("/reports/occupancy", ctx -> {
            String raw = ctx.queryParam("date");
            if (raw == null || !DATE.matcher(raw).matches()) throw ApiException.invalid("Parâmetro date deve ser AAAA-MM-DD");
            csv(ctx, reports.occupancy(parseDate(raw, "date")));
        });
        app.get("/reports/revenue", ctx -> {
            String raw = ctx.queryParam("month");
            if (raw == null || !MONTH.matcher(raw).matches()) throw ApiException.invalid("Parâmetro month deve ser AAAA-MM");
            try {
                csv(ctx, reports.revenue(YearMonth.parse(raw)));
            } catch (DateTimeParseException e) {
                throw ApiException.invalid("Parâmetro month deve ser AAAA-MM");
            }
        });
        return app;
    }

    // ---- entrada ----

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }

    private static ReservationService.NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.body());
        } catch (Exception e) {
            throw ApiException.invalid("Corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("Corpo deve ser um objeto JSON");

        int room = integer(body, "roomNumber");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        String in = text(body, "checkIn");
        String out = text(body, "checkOut");
        int guests = integer(body, "guests");
        String plan = text(body, "ratePlan");

        LocalDate checkIn = parseDate(in, "checkIn");
        LocalDate checkOut = parseDate(out, "checkOut");
        if (!checkOut.isAfter(checkIn)) throw ApiException.invalid("A saída deve ser depois da entrada");
        if (guests < 1) throw ApiException.invalid("É preciso pelo menos um hóspede");
        RatePlan ratePlan;
        try {
            ratePlan = RatePlan.valueOf(plan);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan inválido");
        }
        return new ReservationService.NewReservation(room, name, email, cpf, checkIn, checkOut, guests, ratePlan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) throw ApiException.invalid("Campo obrigatório: " + field);
        return n.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório (inteiro): " + field);
        }
        return n.asInt();
    }

    private static LocalDate parseDate(String raw, String field) {
        if (!DATE.matcher(raw).matches()) throw ApiException.invalid("Data inválida em " + field);
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw ApiException.invalid("Data inválida em " + field);
        }
    }

    // ---- saída ----

    private static ObjectNode reservation(Reservation r) {
        ObjectNode n = JSON.createObjectNode();
        n.put("id", r.id()).put("roomNumber", r.roomNumber()).put("guestName", r.guestName())
         .put("guestEmail", r.guestEmail()).put("guestCpf", r.guestCpf())
         .put("checkIn", r.checkIn().toString()).put("checkOut", r.checkOut().toString())
         .put("guests", r.guests()).put("ratePlan", r.ratePlan().name()).put("nights", r.nights())
         .put("subtotal", r.subtotal()).put("discount", r.discount()).put("total", r.total())
         .put("status", r.status().name()).put("amountPaid", r.amountPaid());
        if (r.refund() == null) n.putNull("refund"); else n.put("refund", r.refund());
        return n;
    }

    private static ObjectNode receipt(Receipt d) {
        ObjectNode n = JSON.createObjectNode();
        n.put("title", d.title()).put("reservationId", d.reservationId()).put("guestName", d.guestName())
         .put("guestCpf", d.guestCpf()).put("roomNumber", d.roomNumber()).put("roomType", d.roomType())
         .put("checkIn", d.checkIn().toString()).put("checkOut", d.checkOut().toString())
         .put("nights", d.nights()).put("subtotal", d.subtotal()).put("discount", d.discount())
         .put("total", d.total());
        return n;
    }

    private static void json(Context ctx, int status, JsonNode node) {
        ctx.status(status).contentType("application/json; charset=utf-8").result(node.toString());
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static void error(Context ctx, int status, String code, String message) {
        ObjectNode n = JSON.createObjectNode();
        n.put("error", code).put("message", message);
        json(ctx, status, n);
    }
}
