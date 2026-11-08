package pousada.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pousada.domain.ApiException;
import pousada.domain.ErrorCode;
import pousada.report.Report;
import pousada.report.ReportService;
import pousada.service.ReservationService;

import java.util.List;
import java.util.Map;

public final class Api {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);

    private final ReservationService reservations;
    private final ReportService reportService;
    private final List<Report> reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    private final ReservationRequestParser parser = new ReservationRequestParser(mapper);

    public Api(ReservationService reservations, ReportService reportService, List<Report> reports) {
        this.reservations = reservations;
        this.reportService = reportService;
        this.reports = reports;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(mapper, false));
        });

        app.get("/rooms", ctx -> ctx.json(reservations.rooms().stream().map(RoomView::of).toList()));
        app.post("/reservations", ctx ->
                ctx.status(201).json(ReservationView.of(reservations.create(parser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx ->
                ctx.json(ReservationView.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx ->
                ctx.json(ReservationView.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx ->
                ctx.json(ReservationView.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(reservations.messages()));

        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> ctx
                    .contentType("text/csv; charset=utf-8")
                    .result(reportService.generate(report, ctx.queryParam(report.periodKind().parameter()))));
        }

        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(), e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            error(ctx, 500, "INTERNAL_ERROR", "Erro interno");
        });
        return app;
    }

    private static long id(Context ctx) {
        String text = ctx.pathParam("id");
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + text + " não existe");
        }
    }

    private static void error(Context ctx, int status, String code, String message) {
        ctx.status(status).json(Map.of("error", code, "message", message));
    }
}
