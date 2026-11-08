package br.com.marealta.report;

import br.com.marealta.store.Store;
import java.math.BigDecimal;
import java.util.List;

public final class Reports {
    private Reports() {}

    /** Todos os relatórios do sistema. Um relatório novo entra aqui. */
    public static List<Report> all(Store store, BigDecimal tourismTaxPerGuestNight) {
        return List.of(
                new OccupancyReport(store),
                new RevenueReport(store),
                new TourismTaxReport(store, tourismTaxPerGuestNight),
                new RatePlansReport(store));
    }
}
