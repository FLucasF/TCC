package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.DomainException;
import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.ReservationService;
import br.com.marealta.reservas.domain.Room;
import br.com.marealta.reservas.domain.RoomCatalog;
import br.com.marealta.reservas.notification.MessageOutbox;
import br.com.marealta.reservas.report.OccupancyReport;
import br.com.marealta.reservas.report.RevenueReport;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Api {
    private static final String CSV = "text/csv; charset=utf-8";

    private final RoomCatalog rooms;
    private final ReservationService reservations;
    private final MessageOutbox outbox;
    private final OccupancyReport occupancy;
    private final RevenueReport revenue;

    public Api(RoomCatalog rooms, ReservationService reservations, MessageOutbox outbox,
               OccupancyReport occupancy, RevenueReport revenue) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.outbox = outbox;
        this.occupancy = occupancy;
        this.revenue = revenue;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create();
        app.exception(DomainException.class, (e, ctx) -> ctx.status(e.httpStatus())
                .json(Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> ctx.status(500)
                .json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno")));

        app.get("/rooms", ctx -> ctx.json(rooms.all().stream().map(RoomView::of).toList()));
        app.post("/reservations", ctx -> ctx.status(201).json(ReservationView.of(
                reservations.create(ReservationRequestParser.parse(ctx.body())))));
        app.get("/reservations/{id}", ctx -> ctx.json(ReservationView.of(reservations.get(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(ReservationView.of(reservations.confirm(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(ReservationView.of(reservations.cancel(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(ReservationView.of(reservations.checkIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx ->
                ctx.json(ReceiptView.of(Receipt.of(reservations.checkOut(id(ctx))))));
        app.get("/reservations/{id}/document", ctx ->
                ctx.json(ReceiptView.of(Receipt.of(reservations.paidReservation(id(ctx))))));
        app.get("/messages", ctx -> ctx.json(outbox.sent()));
        app.get("/reports/occupancy", ctx -> ctx.contentType(CSV)
                .result(occupancy.generate(reservations.all(), dateParam(ctx, "date"))));
        app.get("/reports/revenue", ctx -> ctx.contentType(CSV)
                .result(revenue.generate(reservations.all(), monthParam(ctx, "month"))));
        return app.start(port);
    }

    private static String id(Context ctx) {
        return ctx.pathParam("id");
    }

    private static LocalDate dateParam(Context ctx, String name) {
        try {
            return LocalDate.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Data fora do formato AAAA-MM-DD: " + name);
        }
    }

    private static YearMonth monthParam(Context ctx, String name) {
        try {
            return YearMonth.parse(required(ctx, name));
        } catch (DateTimeParseException e) {
            throw DomainException.invalidRequest("Mês fora do formato AAAA-MM: " + name);
        }
    }

    private static String required(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null) throw DomainException.invalidRequest("Parâmetro obrigatório ausente: " + name);
        return value;
    }
}
