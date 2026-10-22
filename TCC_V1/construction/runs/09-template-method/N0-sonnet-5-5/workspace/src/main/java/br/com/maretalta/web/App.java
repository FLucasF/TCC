package br.com.maretalta.web;

import br.com.maretalta.domain.Message;
import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.Receipt;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Room;
import br.com.maretalta.report.ReportType;
import br.com.maretalta.report.Reports;
import br.com.maretalta.service.ApiException;
import br.com.maretalta.service.ReservationService;
import br.com.maretalta.service.ReservationService.NewReservation;
import br.com.maretalta.store.Store;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

public final class App {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true)); // 0.00 e não 0

    private App() {}

    public static Javalin create(Store store, BigDecimal tourismTaxPerGuestNight) {
        ReservationService service = new ReservationService(store);
        Reports reports = new Reports(store, tourismTaxPerGuestNight);

        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code(), e.getMessage()));
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

        app.get("/rooms", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Room r : Room.ALL) {
                arr.add(JSON.createObjectNode().put("number", r.number()).put("type", r.type().name())
                        .put("capacity", r.capacity()).put("nightlyRate", r.nightlyRate()));
            }
            json(ctx, 200, arr);
        });

        app.post("/reservations", ctx -> json(ctx, 201, reservation(service.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservation(service.get(ctx.pathParam("id")))));
        action(app, "confirmation", service::confirm);
        action(app, "cancellation", service::cancel);
        action(app, "check-in", service::checkIn);
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, receipt(service.checkOut(ctx.pathParam("id")))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, receipt(service.document(ctx.pathParam("id")))));

        app.get("/messages", ctx -> {
            ArrayNode arr = JSON.createArrayNode();
            for (Message m : service.messages()) {
                arr.add(JSON.createObjectNode().put("id", m.id()).put("channel", m.channel()).put("to", m.to())
                        .put("subject", m.subject()).put("body", m.body()));
            }
            json(ctx, 200, arr);
        });

        for (ReportType type : reports.types()) {
            app.get("/reports/" + type.name(), ctx -> {
                String csv = reports.csv(type, ctx.queryParam(type.periodParam()));
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            });
        }
        return app;
    }

    private static void action(Javalin app, String path, Function<String, Reservation> op) {
        app.post("/reservations/{id}/" + path, ctx -> json(ctx, 200, reservation(op.apply(ctx.pathParam("id")))));
    }

    // ---- entrada ----

    private static NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.bodyAsBytes());
        } catch (java.io.IOException e) {
            throw ApiException.invalid("O corpo deve ser um JSON válido");
        }
        if (body == null || !body.isObject()) throw ApiException.invalid("O corpo deve ser um objeto JSON");
        return new NewReservation(
                integer(body, "roomNumber"),
                text(body, "guestName"), text(body, "guestEmail"), text(body, "guestCpf"),
                date(body, "checkIn"), date(body, "checkOut"),
                integer(body, "guests"),
                ratePlan(body));
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText().strip();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw ApiException.invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.intValue();
    }

    private static LocalDate date(JsonNode body, String field) {
        String s = text(body, field);
        try {
            if (!s.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
            return LocalDate.parse(s);
        } catch (RuntimeException e) {
            throw ApiException.invalid("Data inválida em " + field + " (use 2027-07-10)");
        }
    }

    private static RatePlan ratePlan(JsonNode body) {
        String s = text(body, "ratePlan");
        try {
            return RatePlan.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ApiException.invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
    }

    // ---- saída ----

    private static ObjectNode reservation(Reservation r) {
        ObjectNode o = JSON.createObjectNode();
        o.put("id", r.id()).put("roomNumber", r.roomNumber()).put("guestName", r.guestName())
                .put("guestEmail", r.guestEmail()).put("guestCpf", r.guestCpf())
                .put("checkIn", r.checkIn().toString()).put("checkOut", r.checkOut().toString())
                .put("guests", r.guests()).put("ratePlan", r.ratePlan().name()).put("nights", r.nights())
                .put("subtotal", r.subtotal()).put("discount", r.discount()).put("total", r.total())
                .put("status", r.status().name()).put("amountPaid", r.amountPaid());
        if (r.refund() == null) o.putNull("refund"); else o.put("refund", r.refund());
        return o;
    }

    private static ObjectNode receipt(Receipt d) {
        return JSON.createObjectNode().put("title", Receipt.TITLE).put("reservationId", d.reservationId())
                .put("guestName", d.guestName()).put("guestCpf", d.guestCpf()).put("roomNumber", d.roomNumber())
                .put("roomType", d.roomType().name()).put("checkIn", d.checkIn().toString())
                .put("checkOut", d.checkOut().toString()).put("nights", d.nights())
                .put("subtotal", d.subtotal()).put("discount", d.discount()).put("total", d.total());
    }

    private static void json(Context ctx, int status, JsonNode node) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(node));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void error(Context ctx, int status, String code, String message) {
        json(ctx, status, JSON.createObjectNode().put("error", code).put("message", message));
    }
}
