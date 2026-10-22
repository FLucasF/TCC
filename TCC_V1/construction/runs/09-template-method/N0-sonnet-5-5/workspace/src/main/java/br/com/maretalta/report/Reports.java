package br.com.maretalta.report;

import br.com.maretalta.service.ApiException;
import br.com.maretalta.store.Store;

import java.math.BigDecimal;
import java.util.List;

/** Registro dos relatórios disponíveis e geração do CSV. */
public class Reports {
    private final List<ReportType> types;
    private final Store store;

    public Reports(Store store, BigDecimal tourismTaxPerGuestNight) {
        this.store = store;
        this.types = all(tourismTaxPerGuestNight);
    }

    /** Novo relatório (cancelamentos, hóspedes por estado...)? Acrescente aqui. */
    static List<ReportType> all(BigDecimal tourismTaxPerGuestNight) {
        return List.of(
                new OccupancyReport(),
                new RevenueReport(),
                new TourismTaxReport(tourismTaxPerGuestNight),
                new RatePlanReport());
    }

    public List<ReportType> types() { return types; }

    public String csv(ReportType type, String rawPeriod) {
        return store.inTransaction(tx -> ReportWriter.toCsv(type.build(tx, rawPeriod)));
    }

    public String csv(String name, String rawPeriod) {
        ReportType type = types.stream().filter(t -> t.name().equals(name)).findFirst()
                .orElseThrow(() -> ApiException.invalid("Relatório desconhecido: " + name));
        return csv(type, rawPeriod);
    }
}
