package br.com.marealta.report;

import br.com.marealta.service.RoomCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Único lugar que conhece todos os relatórios. */
public class ReportRegistry {
    private final Map<String, Report> byName = new LinkedHashMap<>();

    public ReportRegistry(List<Report> reports) {
        reports.forEach(r -> byName.put(r.name(), r));
    }

    public static ReportRegistry standard(RoomCatalog rooms, TourismTaxPolicy taxPolicy) {
        return new ReportRegistry(List.of(
                new OccupancyReport(rooms),
                new RevenueReport(),
                new TourismTaxReport(taxPolicy),
                new RatePlanReport()));
    }

    public Optional<Report> find(String name) { return Optional.ofNullable(byName.get(name)); }
}
