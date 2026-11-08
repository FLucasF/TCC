package pousada;

import pousada.domain.RoomCatalog;
import pousada.persistence.Database;
import pousada.report.OccupancyReport;
import pousada.report.RatePlanReport;
import pousada.report.Report;
import pousada.report.ReportService;
import pousada.report.RevenueReport;
import pousada.report.TourismTaxReport;
import pousada.service.ReservationService;
import pousada.web.Api;

import java.math.BigDecimal;
import java.util.List;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        Database database = new Database(required("DB_URL"), env("DB_USER", null), env("DB_PASSWORD", null));
        database.createSchema();

        RoomCatalog rooms = RoomCatalog.standard();
        // Novo relatório: uma classe que implementa Report e uma linha aqui.
        List<Report> reports = List.of(
                new OccupancyReport(rooms),
                new RevenueReport(),
                new TourismTaxReport(new BigDecimal(env("TOURISM_TAX_PER_GUEST_NIGHT", "4.50"))),
                new RatePlanReport());

        Api api = new Api(new ReservationService(database, rooms), new ReportService(database), reports);
        api.create().start(Integer.parseInt(env("PORT", "7070")));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente: " + name);
        }
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
