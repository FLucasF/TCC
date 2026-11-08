package com.pousada.report;

import java.util.ArrayList;
import java.util.List;

public abstract class ReportGenerator {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public String generate(String period) {
        List<String> lines = new ArrayList<>();

        lines.add(generateTitle(period));
        lines.add(generateHeader());
        lines.addAll(generateDataLines(period));
        lines.add(generateTotal(period));

        return String.join("\n", lines);
    }

    private String generateTitle(String period) {
        return String.format("%s,%s,%s", POUSADA_NAME, getReportName(), period);
    }

    protected abstract String getReportName();

    protected abstract String generateHeader();

    protected abstract List<String> generateDataLines(String period);

    protected abstract String generateTotal(String period);

    protected String formatMoney(java.math.BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    protected String formatDate(java.time.LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
