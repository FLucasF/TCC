package com.pousada.reports;

import java.time.LocalDate;
import java.util.List;

public abstract class Report {
    protected static final String LODGE_NAME = "Pousada Maré Alta";

    public String generate(LocalDate date) throws Exception {
        StringBuilder csv = new StringBuilder();

        csv.append(generateTitle(date)).append("\n");
        csv.append(generateHeader()).append("\n");

        List<String> rows = generateRows(date);
        for (String row : rows) {
            csv.append(row).append("\n");
        }

        csv.append(generateTotal(date));

        return csv.toString();
    }

    protected abstract String getReportName();
    protected abstract String getReportPeriod(LocalDate date);
    protected abstract String generateHeader();
    protected abstract List<String> generateRows(LocalDate date) throws Exception;
    protected abstract String generateTotal(LocalDate date) throws Exception;

    private String generateTitle(LocalDate date) {
        return String.format("%s,%s,%s", LODGE_NAME, getReportName(), getReportPeriod(date));
    }
}
