package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import pousada.app.*;
import pousada.domain.*;
import pousada.infra.MessageLog;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static pousada.domain.DomainException.Code.*;

public class Api {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageLog messages;
    private final RoomCatalog rooms;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public Api(ReservationService reservations, ReportService reports, MessageLog messages, RoomCatalog rooms) {
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
        this.rooms = rooms;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(RoomView::of).toList()));
        app.post("/reservations", ctx -> ctx.status(201).json(ReservationView.of(reservations.create(parseNew(ctx)))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationView.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationView.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationView.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(messages.all()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV)
                .result(reports.occupancy(date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV)
                .result(reports.revenue(month(ctx.queryParam("month")))));

        app.exception(DomainException.class, (e, ctx) -> error(ctx, statusOf(e.code()), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app.start(port);
    }

    private static int statusOf(DomainException.Code code) {
        return switch (code) {
            case INVALID_REQUEST -> 400;
            case ROOM_NOT_FOUND, RESERVATION_NOT_FOUND -> 404;
            case ROOM_UNAVAILABLE, OPERATION_NOT_ALLOWED -> 409;
            case CAPACITY_EXCEEDED -> 422;
        };
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(RESERVATION_NOT_FOUND, "Reserva " + ctx.pathParam("id") + " não existe");
        }
    }

    private NewReservation parseNew(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw invalid("O corpo precisa ser um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw invalid("O corpo precisa ser um objeto JSON");
        }
        Guest guest = new Guest(text(body, "guestName"), text(body, "guestEmail"), text(body, "guestCpf"));
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan inválido");
        }
        return new NewReservation(integer(body, "roomNumber"), guest, date(text(body, "checkIn"), "checkIn"),
                date(text(body, "checkOut"), "checkOut"), integer(body, "guests"), plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo " + field + " é obrigatório");
        }
        return node.asText().trim();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo " + field + " é obrigatório e deve ser um número inteiro");
        }
        return node.asInt();
    }

    private static LocalDate date(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("Data inválida em " + field + ", use o formato 2027-07-10");
        }
    }

    private static YearMonth month(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw invalid("Mês inválido, use o formato 2027-07");
        }
    }

    private static DomainException invalid(String message) {
        return new DomainException(INVALID_REQUEST, message);
    }
}
