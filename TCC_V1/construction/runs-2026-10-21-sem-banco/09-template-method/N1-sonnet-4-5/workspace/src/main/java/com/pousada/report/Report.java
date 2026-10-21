package com.pousada.report;

import java.util.List;

public abstract class Report {
    protected static final String POUSADA_NAME = "Pousada Maré Alta";

    public String generate() {
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

    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract String buildHeaderLine();
    protected abstract List<String> buildDataLines();
    protected abstract String buildTotalLine();

    private String buildTitleLine() {
        return String.format("%s,%s,%s", POUSADA_NAME, getReportName(), getPeriod());
    }
}
