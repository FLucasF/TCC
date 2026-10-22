package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.DomainException;
import br.com.marealta.pousada.domain.ErrorCode;
import br.com.marealta.pousada.domain.Rooms;
import br.com.marealta.pousada.service.Report;
import br.com.marealta.pousada.service.ReportService;
import br.com.marealta.pousada.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.function.LongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private final ReservationService reservations;
    private final ReportService reports;
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();
    private final ReservationRequestParser parser = new ReservationRequestParser(mapper);

    public App(ReservationService reservations, ReportService reports) {
        this.reservations = reservations;
        this.reports = reports;
    }

    public Javalin create(Runnable onStopped) {
        Javalin app = Javalin.create(config -> config.events(events -> events.serverStopped(onStopped::run)));
        app.get("/rooms", ctx -> json(ctx, 200, Rooms.ALL.stream().map(Views.RoomView::of).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                Views.ReservationView.of(reservations.create(parser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> reservation(ctx, reservations::get));
        app.post("/reservations/{id}/confirmation", ctx -> reservation(ctx, reservations::confirm));
        app.post("/reservations/{id}/cancellation", ctx -> reservation(ctx, reservations::cancel));
        app.post("/reservations/{id}/check-in", ctx -> reservation(ctx, reservations::checkIn));
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, reservations.document(id(ctx))));
        app.get("/messages", ctx -> json(ctx, 200, reservations.messages()));
        app.get("/reports/occupancy", ctx -> csv(ctx, reports.occupancy(queryDate(ctx, "date"))));
        app.get("/reports/revenue", ctx -> csv(ctx, reports.revenue(queryMonth(ctx, "month"))));

        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private void reservation(Context ctx, LongFunction<br.com.marealta.pousada.domain.Reservation> operation) {
        json(ctx, 200, Views.ReservationView.of(operation.apply(id(ctx))));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + ctx.pathParam("id") + " não existe");
        }
    }

    private static LocalDate queryDate(Context ctx, String name) {
        try {
            return LocalDate.parse(String.valueOf(ctx.queryParam(name)));
        } catch (DateTimeParseException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Parâmetro " + name + " ausente ou inválido (use 2027-07-10)");
        }
    }

    private static YearMonth queryMonth(Context ctx, String name) {
        try {
            return YearMonth.parse(String.valueOf(ctx.queryParam(name)));
        } catch (DateTimeParseException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Parâmetro " + name + " ausente ou inválido (use 2027-07)");
        }
    }

    private void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void error(Context ctx, int status, String code, String message) {
        json(ctx, status, new Views.ErrorView(code, message));
    }

    private static void csv(Context ctx, Report report) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(report.toCsv());
    }
}
