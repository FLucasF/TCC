package com.pousada.report;

import java.util.List;

public abstract class Report {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public final String generate() {
        StringBuilder csv = new StringBuilder();

        csv.append(buildTitleLine()).append("\n");
        csv.append(buildHeaderLine()).append("\n");

        List<String> dataLines = buildDataLines();
        for (String line : dataLines) {
            csv.append(line).append("\n");
        }

        csv.append(buildTotalLine());

        return csv.toString();
    }

    private String buildTitleLine() {
        return String.join(",", POUSADA_NAME, getReportName(), getPeriod());
    }

    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract String buildHeaderLine();
    protected abstract List<String> buildDataLines();
    protected abstract String buildTotalLine();
}
