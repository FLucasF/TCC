package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.ApiException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.service.Outbox;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Routes {
    private static final String CSV = "text/csv; charset=utf-8";

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ReservationService reservations;
    private final ReportService reports;
    private final Outbox outbox;

    public Routes(ReservationService reservations, ReportService reports, Outbox outbox) {
        this.reservations = reservations;
        this.reports = reports;
        this.outbox = outbox;
    }

    public void register(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));

        app.get("/rooms", ctx -> json(ctx, 200, reservations.listRooms()));
        app.post("/reservations", ctx ->
                json(ctx, 201, reservations.create(ReservationRequest.parse(mapper, ctx.body()))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, reservations.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> json(ctx, 200, reservations.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> json(ctx, 200, reservations.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> json(ctx, 200, reservations.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, outbox.list()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(date(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(month(ctx.queryParam("month")))));
    }

    private void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
    }

    private void csv(Context ctx, String body) {
        ctx.status(200).contentType(CSV).result(body);
    }

    private void error(Context ctx, ErrorCode code, String message) {
        try {
            json(ctx, code.httpStatus(), Map.of("error", code.name(), "message", message));
        } catch (Exception e) {
            ctx.status(500);
        }
    }

    private long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + ctx.pathParam("id") + " não existe");
        }
    }

    private LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Informe date no formato 2027-07-10");
        }
    }

    private YearMonth month(String value) {
        try {
            if (value == null || !value.matches("\\d{4}-\\d{2}")) {
                throw new DateTimeParseException("formato", String.valueOf(value), 0);
            }
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Informe month no formato 2027-07");
        }
    }
}
