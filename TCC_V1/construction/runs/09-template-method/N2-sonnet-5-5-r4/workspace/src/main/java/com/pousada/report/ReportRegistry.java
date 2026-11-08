package com.pousada.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** O único lugar onde se sabe quais relatórios existem. */
public class ReportRegistry {
    private final Map<String, Report<?>> reports = new LinkedHashMap<>();

    public ReportRegistry(List<Report<?>> available) {
        available.forEach(r -> reports.put(r.name(), r));
    }

    public Optional<Report<?>> find(String name) {
        return Optional.ofNullable(reports.get(name));
    }
}
