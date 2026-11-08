package br.com.mare.web;

import br.com.mare.domain.BusinessException;
import br.com.mare.domain.Hotel;
import br.com.mare.domain.Receipt;
import br.com.mare.domain.Reservation;
import br.com.mare.domain.ReservationService;
import br.com.mare.persistence.JdbcMessageRepository;
import br.com.mare.report.Report;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class Api {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    record RoomView(int number, String type, int capacity, BigDecimal nightlyRate) {
    }

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                           BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan().name(), r.nights(), r.subtotal(),
                    r.discount(), r.total(), r.status().name(), r.amountPaid(), r.refund());
        }
    }

    private Api() {
    }

    public static Javalin create(ReservationService service, JdbcMessageRepository messages,
                                 List<Report<?>> reports) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson(MAPPER, false));
        });

        app.exception(BusinessException.class, (e, ctx) -> ctx.status(e.code().httpStatus())
                .json(Map.of("error", e.code().name(), "message", e.getMessage())));

        app.get("/rooms", ctx -> ctx.json(Hotel.rooms().stream()
                .map(r -> new RoomView(r.number(), r.type().name(), r.capacity(), r.nightlyRate())).toList()));

        app.post("/reservations", ctx -> {
            RequestParser.NewReservation n = RequestParser.parse(MAPPER, ctx.body());
            Reservation created = service.create(n.roomNumber(), n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan());
            ctx.status(201).json(ReservationView.of(created));
        });
        app.get("/reservations/{id}", ctx -> reservation(ctx, service.find(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> reservation(ctx, service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> reservation(ctx, service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> reservation(ctx, service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> receipt(ctx, service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> receipt(ctx, service.document(id(ctx))));

        app.get("/messages", ctx -> ctx.json(messages.findAll()));

        for (Report<?> report : reports) {
            app.get("/reports/" + report.name(), ctx -> ctx
                    .contentType("text/csv; charset=utf-8")
                    .result(report.render(ctx.queryParam(report.parameter()))));
        }
        return app;
    }

    private static String id(Context ctx) {
        return ctx.pathParam("id");
    }

    private static void reservation(Context ctx, Reservation r) {
        ctx.json(ReservationView.of(r));
    }

    private static void receipt(Context ctx, Receipt r) {
        ctx.json(r);
    }
}
