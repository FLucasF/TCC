package br.com.pousadamarealta.reservas.web;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.RoomCatalog;
import br.com.pousadamarealta.reservas.messaging.MessagePipeline;
import br.com.pousadamarealta.reservas.messaging.MessageRules;
import br.com.pousadamarealta.reservas.messaging.Outbox;
import br.com.pousadamarealta.reservas.report.OccupancyReport;
import br.com.pousadamarealta.reservas.report.RevenueReport;
import br.com.pousadamarealta.reservas.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.json.JavalinJackson;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.LongFunction;

public final class App {

    private static final String CSV = "text/csv; charset=utf-8";

    private final RoomCatalog catalog = RoomCatalog.marealta();
    private final Outbox outbox;
    private final ReservationService reservations;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new SimpleModule().addSerializer(LocalDate.class, ToStringSerializer.instance))
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

    private App(Config config) {
        this.outbox = new Outbox(new MessagePipeline(config.testMode(), MessageRules.marealta()));
        this.reservations = new ReservationService(catalog, outbox);
    }

    public static Javalin create(Config config) {
        return new App(config).javalin();
    }

    private Javalin javalin() {
        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.jsonMapper(new JavalinJackson(mapper, false));
        });
        app.exception(DomainException.class, (e, ctx) -> error(ctx, e.code().httpStatus(), e.code().name(), e));
        app.exception(Exception.class, (e, ctx) -> error(ctx, 500, "INTERNAL_ERROR", e));

        app.get("/rooms", ctx -> ctx.json(catalog.all()));
        app.post("/reservations", ctx -> ctx.status(201).json(ReservationView.of(
                reservations.create(Requests.newReservation(mapper, ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", reservationAction(reservations::confirm));
        app.post("/reservations/{id}/cancellation", reservationAction(reservations::cancel));
        app.post("/reservations/{id}/check-in", reservationAction(reservations::checkIn));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(reservations.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(reservations.document(id(ctx))));
        app.get("/messages", ctx -> ctx.json(outbox.messages()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV).result(OccupancyReport.render(
                reservations.all(), catalog, Requests.date(ctx.queryParam("date"), "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV).result(RevenueReport.render(
                reservations.all(), Requests.month(ctx.queryParam("month")))));
        return app;
    }

    private Handler reservationAction(LongFunction<Reservation> action) {
        return ctx -> ctx.json(ReservationView.of(action.apply(id(ctx))));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void error(Context ctx, int status, String code, Exception e) {
        ctx.status(status).json(Map.of("error", code, "message", String.valueOf(e.getMessage())));
    }
}
