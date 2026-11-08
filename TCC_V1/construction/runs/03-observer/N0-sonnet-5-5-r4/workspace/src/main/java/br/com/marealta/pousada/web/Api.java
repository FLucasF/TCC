package br.com.marealta.pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import br.com.marealta.pousada.domain.AppException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import br.com.marealta.pousada.service.ReservationService.NewReservation;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Api {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private final ReservationService reservations;
    private final ReportService reports;

    public Api(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(cfg -> cfg.showJavalinBanner = false);

        app.get("/rooms", ctx -> json(ctx, 200, reservations.rooms()));
        app.post("/reservations", ctx -> json(ctx, 201, reservations.create(parseNewReservation(ctx))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, reservations.messages()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(month(ctx.queryParam("month")))));

        app.exception(AppException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            ctx.status(500).contentType("application/json")
                    .result("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Erro interno\"}");
        });
        return app;
    }

    private static void error(Context ctx, ErrorCode code, String message) {
        json(ctx, code.httpStatus(), Map.of("error", code.name(), "message", message));
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(JSON.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String body) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(body);
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + ctx.pathParam("id") + " não existe");
        }
    }

    private static LocalDate date(String value, String field) {
        if (value == null) {
            throw invalid("Falta o parâmetro " + field);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + ": use o formato 2027-07-10");
        }
    }

    private static YearMonth month(String value) {
        if (value == null) {
            throw invalid("Falta o parâmetro month");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Mês inválido: use o formato 2027-07");
        }
    }

    private static NewReservation parseNewReservation(Context ctx) {
        JsonNode body;
        try {
            body = JSON.readTree(ctx.body());
        } catch (JsonProcessingException e) {
            throw invalid("O corpo não é um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw invalid("O corpo deve ser um objeto JSON");
        }
        LocalDate checkIn = date(text(body, "checkIn"), "checkIn");
        LocalDate checkOut = date(text(body, "checkOut"), "checkOut");
        int guests = integer(body, "guests");
        int roomNumber = integer(body, "roomNumber");
        String guestName = text(body, "guestName");
        String guestEmail = text(body, "guestEmail");
        String guestCpf = text(body, "guestCpf");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("ratePlan deve ser STANDARD ou NON_REFUNDABLE");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("Deve haver ao menos um hóspede");
        }
        return new NewReservation(roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isTextual() || n.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode n = body.get(field);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return n.intValue();
    }

    private static AppException invalid(String message) {
        return new AppException(ErrorCode.INVALID_REQUEST, message);
    }
}
