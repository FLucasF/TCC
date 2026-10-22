package pousada.report;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Único lugar que lista os relatórios existentes: um novo relatório entra com um register(). */
public class ReportRegistry {
    private final Map<String, Report<?>> reports = new LinkedHashMap<>();

    public ReportRegistry register(Report<?> report) {
        reports.put(report.name(), report);
        return this;
    }

    public Optional<Report<?>> find(String name) {
        return Optional.ofNullable(reports.get(name));
    }
}
