package pousada.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import pousada.db.Database;
import pousada.db.MessageRepository;
import pousada.db.ReservationRepository;
import pousada.db.RoomRepository;
import pousada.domain.ReservationService;
import pousada.report.CsvReportWriter;
import pousada.report.OccupancyReport;
import pousada.report.RatePlanReport;
import pousada.report.Report;
import pousada.report.RevenueReport;
import pousada.report.TourismTaxReport;
import pousada.error.ApiException;

/** Monta o sistema. Relatório novo entra na lista de {@link #reports}. */
public final class App {
    /** Taxa de turismo da prefeitura, por hóspede por noite. */
    private static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    private App() {
    }

    public static Javalin create(Database db) {
        RoomRepository rooms = new RoomRepository(db);
        ReservationRepository reservations = new ReservationRepository(db);
        MessageRepository messages = new MessageRepository(db);
        ReservationService service = new ReservationService(db, rooms, reservations, messages);

        List<Report> reports = List.of(
                new OccupancyReport(reservations, rooms),
                new RevenueReport(reservations),
                new TourismTaxReport(reservations, TOURISM_TAX_PER_GUEST_NIGHT),
                new RatePlanReport(reservations));

        Javalin app = Javalin.create(config -> config.jsonMapper(Json.JAVALIN_MAPPER));
        app.exception(ApiException.class, (e, ctx) ->
                ctx.status(e.status()).json(Map.of("error", e.code(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });

        app.get("/rooms", ctx -> ctx.json(rooms.all()));
        app.get("/messages", ctx -> ctx.json(messages.all()));
        ReservationRoutes.register(app, service);
        ReportRoutes.register(app, reports, new CsvReportWriter());
        return app;
    }
}
