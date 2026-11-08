package br.com.marealta.report;

import br.com.marealta.store.Store;

import java.util.List;

public final class ReportService {
    private final Store store;
    private final List<Report> reports;

    public ReportService(Store store, List<Report> reports) {
        this.store = store;
        this.reports = reports;
    }

    /** All reports available; add new ones here. */
    public static List<Report> standardReports(TourismTax tax) {
        return List.of(new OccupancyReport(), new RevenueReport(), new TourismTaxReport(tax),
                new RatePlanReport());
    }

    public List<Report> reports() {
        return reports;
    }

    public String csv(Report report, String period) {
        return CsvFormat.render(store.inTransaction(tx -> report.generate(period, tx)));
    }
}
