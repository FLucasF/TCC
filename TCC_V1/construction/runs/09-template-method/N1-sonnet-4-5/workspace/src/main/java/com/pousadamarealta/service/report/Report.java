package com.pousadamarealta.service.report;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Report {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(buildTitle()).append("\n");
        csv.append(buildHeader()).append("\n");

        List<String> dataLines = buildDataLines();
        for (String line : dataLines) {
            csv.append(line).append("\n");
        }

        csv.append(buildTotal());

        return csv.toString();
    }

    private String buildTitle() {
        return String.format("%s,%s,%s", POUSADA_NAME, getReportName(), getPeriod());
    }

    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract String buildHeader();
    protected abstract List<String> buildDataLines();
    protected abstract String buildTotal();

    protected String formatCsvValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    protected String formatCsvDate(java.time.LocalDate date) {
        return date.toString();
    }

    protected String formatCsvMoney(java.math.BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    protected String joinCsvFields(Object... fields) {
        return java.util.Arrays.stream(fields)
                .map(this::formatCsvValue)
                .collect(Collectors.joining(","));
    }
}
