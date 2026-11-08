package pousada.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import pousada.domain.AppException;
import pousada.domain.Reservation;
import pousada.domain.RoomCatalog;
import pousada.repo.MessageRepository;
import pousada.reports.Report;
import pousada.service.NewReservation;
import pousada.service.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class Api {
    private Api() {
    }

    public record RoomView(int number, String type, int capacity, BigDecimal nightlyRate) {
    }

    public record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                                  LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                                  BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan().name(), r.nights(), r.subtotal(),
                    r.discount(), r.total(), r.status().name(), r.amountPaid(), r.refund());
        }
    }

    /** Monta o servidor; cada relatório novo entra só na lista {@code reports}. */
    public static Javalin create(ReservationService service, MessageRepository messages, List<Report<?>> reports) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Javalin app = Javalin.create(cfg -> cfg.jsonMapper(new JavalinJackson(mapper, false)));

        app.get("/rooms", ctx -> ctx.json(RoomCatalog.all().stream()
                .map(r -> new RoomView(r.number(), r.type().name(), r.capacity(), r.nightlyRate())).toList()));

        app.post("/reservations", ctx -> {
            JsonNode body;
            try {
                body = mapper.readTree(ctx.body());
            } catch (JsonProcessingException e) {
                throw AppException.invalid("O corpo não é um JSON válido");
            }
            ctx.status(201).json(ReservationView.of(service.create(NewReservation.parse(body))));
        });
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(service.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationView.of(service.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationView.of(service.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationView.of(service.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.receipt(id(ctx))));
        app.get("/messages", ctx -> ctx.json(messages.all()));

        for (Report<?> report : reports) {
            app.get("/reports/" + report.name(), ctx -> {
                String period = ctx.queryParam(report.periodParam());
                if (period == null) {
                    throw AppException.invalid("Falta o parâmetro " + report.periodParam());
                }
                ctx.contentType("text/csv; charset=utf-8").result(report.render(period));
            });
        }

        app.exception(AppException.class, (e, ctx) ->
                ctx.status(e.status()).json(Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app;
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new AppException(404, "RESERVATION_NOT_FOUND", "Reserva não existe");
        }
    }
}
