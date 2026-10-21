package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.BookingException;
import br.com.marealta.reservas.domain.ErrorCode;
import br.com.marealta.reservas.domain.MessageLog;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationService;
import br.com.marealta.reservas.domain.RoomCatalog;
import br.com.marealta.reservas.reports.CsvReportWriter;
import br.com.marealta.reservas.reports.OccupancyReport;
import br.com.marealta.reservas.reports.RatePlanReport;
import br.com.marealta.reservas.reports.Report;
import br.com.marealta.reservas.reports.RevenueReport;
import br.com.marealta.reservas.reports.TourismTaxReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class App {
    public static final BigDecimal DEFAULT_TOURISM_TAX = new BigDecimal("4.50");

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final RequestParser parser = new RequestParser(mapper);
    private final CsvReportWriter csv = new CsvReportWriter();
    private final MessageLog messages = new MessageLog();
    private final ReservationService service = new ReservationService(RoomCatalog.standard(), messages);
    private final List<Report> reports;

    public App(BigDecimal tourismTaxPerGuestNight) {
        this.reports = List.of(
                new OccupancyReport(service),
                new RevenueReport(service),
                new TourismTaxReport(service, tourismTaxPerGuestNight),
                new RatePlanReport(service));
    }

    public Javalin create() {
        Javalin app = Javalin.create();
        app.exception(BookingException.class, (e, ctx) -> error(ctx, e.code(), e.getMessage()));

        app.get("/rooms", ctx -> json(ctx, 200, service.rooms().all().stream().map(Json::room).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                Json.reservation(service.create(parser.parseNewReservation(ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Json.reservation(service.get(id(ctx)))));
        transition(app, "confirmation", service::confirm);
        transition(app, "cancellation", service::cancel);
        transition(app, "check-in", service::checkIn);
        app.post("/reservations/{id}/check-out", ctx -> json(ctx, 200, Json.receipt(service.checkOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> json(ctx, 200, Json.receipt(service.document(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, messages.all().stream().map(Json::message).toList()));

        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> ctx
                    .contentType("text/csv; charset=utf-8")
                    .result(csv.write(report.build(ctx.queryParam(report.periodParam())))));
        }
        return app;
    }

    private void transition(Javalin app, String path, Function<Integer, Reservation> action) {
        app.post("/reservations/{id}/" + path, ctx -> json(ctx, 200, Json.reservation(action.apply(id(ctx)))));
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new BookingException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva não existe");
        }
    }

    private void json(Context ctx, int status, Object body) throws Exception {
        ctx.status(status).contentType("application/json").result(mapper.writeValueAsString(body));
    }

    private void error(Context ctx, ErrorCode code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code.name());
        body.put("message", message);
        try {
            json(ctx, code.httpStatus(), body);
        } catch (Exception e) {
            ctx.status(500);
        }
    }
}
