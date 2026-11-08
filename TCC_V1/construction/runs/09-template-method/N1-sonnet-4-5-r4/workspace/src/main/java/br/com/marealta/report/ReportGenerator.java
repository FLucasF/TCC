package br.com.marealta.report;

import java.util.List;

public abstract class ReportGenerator {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public String generate(String period) {
        StringBuilder csv = new StringBuilder();

        csv.append(buildTitle(period)).append("\n");
        csv.append(buildHeader()).append("\n");

        List<String> dataRows = buildDataRows(period);
        for (String row : dataRows) {
            csv.append(row).append("\n");
        }

        csv.append(buildTotal(period));

        return csv.toString();
    }

    private String buildTitle(String period) {
        return String.format("%s,%s,%s", POUSADA_NAME, getReportName(), period);
    }

    protected abstract String getReportName();
    protected abstract String buildHeader();
    protected abstract List<String> buildDataRows(String period);
    protected abstract String buildTotal(String period);
}
