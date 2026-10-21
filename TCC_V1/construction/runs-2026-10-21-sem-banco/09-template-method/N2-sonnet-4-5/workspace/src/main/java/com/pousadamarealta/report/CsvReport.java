package com.pousadamarealta.report;

import java.util.List;

public abstract class CsvReport {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(titleLine()).append("\n");
        csv.append(headerLine()).append("\n");
        for (String dataLine : dataLines()) {
            csv.append(dataLine).append("\n");
        }
        csv.append(totalLine());
        return csv.toString();
    }

    protected String titleLine() {
        return String.join(",", POUSADA_NAME, reportName(), period());
    }

    protected abstract String reportName();
    protected abstract String period();
    protected abstract String headerLine();
    protected abstract List<String> dataLines();
    protected abstract String totalLine();
}
