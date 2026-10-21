package pousada.report;

import java.util.List;
import pousada.service.ReservationRepository;
import pousada.service.RoomCatalog;

/** Todo relatório novo é registrado aqui e ganha sua rota sozinho. */
public final class ReportCatalog {
    private ReportCatalog() {
    }

    public static List<Report> all(ReservationRepository repository, RoomCatalog rooms) {
        return List.of(
                new OccupancyReport(repository, rooms),
                new RevenueReport(repository),
                new TourismTaxReport(repository),
                new RatePlanReport(repository));
    }
}
