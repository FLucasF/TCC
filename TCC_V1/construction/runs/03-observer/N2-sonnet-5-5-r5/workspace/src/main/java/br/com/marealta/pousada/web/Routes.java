package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.PousadaException;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.persistence.MessageRepository;
import br.com.marealta.pousada.persistence.RoomRepository;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.web.ApiDtos.ErrorJson;
import br.com.marealta.pousada.web.ApiDtos.ReceiptJson;
import br.com.marealta.pousada.web.ApiDtos.ReservationJson;
import br.com.marealta.pousada.web.ApiDtos.RoomJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class Routes {
    private static final String CSV = "text/csv; charset=utf-8";
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RoomRepository rooms;
    private final ReservationService reservations;
    private final ReportService reports;
    private final MessageRepository messages;

    public Routes(RoomRepository rooms, ReservationService reservations, ReportService reports,
                  MessageRepository messages) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.reports = reports;
        this.messages = messages;
    }

    public void register(Javalin app) {
        app.exception(PousadaException.class, (e, ctx) ->
                ctx.status(e.code().httpStatus()).json(new ErrorJson(e.code().name(), e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(new ErrorJson("INTERNAL_ERROR", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(rooms.findAll().stream().map(RoomJson::of).toList()));

        app.post("/reservations", this::create);
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationJson.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationJson.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationJson.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationJson.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(ReceiptJson.of(reservations.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(ReceiptJson.of(reservations.document(id(ctx)))));

        app.get("/messages", ctx -> ctx.json(messages.findAll()));

        app.get("/reports/occupancy", ctx ->
                ctx.contentType(CSV).result(reports.occupancy(date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx ->
                ctx.contentType(CSV).result(reports.revenue(month(ctx.queryParam("month")))));
    }

    private void create(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.body());
        } catch (Exception e) {
            throw invalid("O corpo precisa ser um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw invalid("O corpo precisa ser um objeto JSON");
        }
        var reservation = reservations.create(
                integer(body, "roomNumber"), text(body, "guestName"), text(body, "guestEmail"),
                text(body, "guestCpf"), date(text(body, "checkIn"), "checkIn"), date(text(body, "checkOut"), "checkOut"),
                integer(body, "guests"), ratePlan(text(body, "ratePlan")));
        ctx.status(HttpStatus.CREATED).json(ReservationJson.of(reservation));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new PousadaException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não encontrada");
        }
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static RatePlan ratePlan(String value) {
        try {
            return RatePlan.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido: " + value);
        }
    }

    private static LocalDate date(String value, String field) {
        try {
            if (value == null) {
                throw invalid("Data ausente: " + field);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Data fora do formato AAAA-MM-DD: " + field);
        }
    }

    private static YearMonth month(String value) {
        try {
            if (value == null || !MONTH.matcher(value).matches()) {
                throw invalid("Mês fora do formato AAAA-MM");
            }
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Mês fora do formato AAAA-MM");
        }
    }

    private static PousadaException invalid(String message) {
        return new PousadaException(ErrorCode.INVALID_REQUEST, message);
    }
}
