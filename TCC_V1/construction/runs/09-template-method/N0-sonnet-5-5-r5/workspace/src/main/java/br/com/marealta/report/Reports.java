package br.com.marealta.report;

import br.com.marealta.domain.TourismTax;
import java.util.List;

public final class Reports {
    private Reports() {}

    /** Todos os relatórios disponíveis; para criar um novo, implemente {@link Report} e acrescente aqui. */
    public static List<Report> all(TourismTax tourismTax) {
        return List.of(
                new OccupancyReport(),
                new RevenueReport(),
                new TourismTaxReport(tourismTax),
                new RatePlanReport());
    }
}
