package pousada.report;

import java.math.BigDecimal;
import java.util.List;

/** Relatórios disponíveis. Um relatório novo entra aqui com uma linha. */
public final class Reports {
    /** Taxa de turismo da prefeitura, por hóspede por noite. */
    static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    private Reports() {
    }

    public static List<Report<?>> all(ReportSource source) {
        return List.of(
                new OccupancyReport(source),
                new RevenueReport(source),
                new TourismTaxReport(source, TOURISM_TAX_PER_GUEST_NIGHT),
                new RatePlanReport(source));
    }
}
