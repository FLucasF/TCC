package com.marealta.report;

import com.marealta.ApiException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** O único lugar que conhece os relatórios: um novo relatório é uma classe e uma linha aqui. */
public final class ReportCatalog {
    public record Definition(String name, String parameter, BiFunction<ReportData, String, Report> factory) {}

    private final Map<String, Definition> definitions = new LinkedHashMap<>();

    public ReportCatalog() {
        add("occupancy", "date", (data, value) -> new OccupancyReport(data, parseDate(value)));
        add("revenue", "month", (data, value) -> new RevenueReport(data, parseMonth(value)));
        add("tourism-tax", "month", (data, value) -> new TourismTaxReport(data, parseMonth(value)));
        add("rate-plans", "month", (data, value) -> new RatePlansReport(data, parseMonth(value)));
    }

    private void add(String name, String parameter, BiFunction<ReportData, String, Report> factory) {
        definitions.put(name, new Definition(name, parameter, factory));
    }

    public Iterable<Definition> all() { return definitions.values(); }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Data ausente ou fora do formato AAAA-MM-DD");
        }
    }

    private static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw ApiException.invalidRequest("Mês ausente ou fora do formato AAAA-MM");
        }
    }
}
