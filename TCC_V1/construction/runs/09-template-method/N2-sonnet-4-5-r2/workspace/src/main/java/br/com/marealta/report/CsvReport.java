package br.com.marealta.report;

import java.util.List;

public abstract class CsvReport {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(buildTitleLine()).append("\n");
        csv.append(buildHeaderLine()).append("\n");
        for (String dataLine : buildDataLines()) {
            csv.append(dataLine).append("\n");
        }
        csv.append(buildTotalLine()).append("\n");
        return csv.toString();
    }

    protected String buildTitleLine() {
        return POUSADA_NAME + "," + getReportName() + "," + getPeriod();
    }

    protected abstract String getReportName();

    protected abstract String getPeriod();

    protected abstract String buildHeaderLine();

    protected abstract List<String> buildDataLines();

    protected abstract String buildTotalLine();
}
