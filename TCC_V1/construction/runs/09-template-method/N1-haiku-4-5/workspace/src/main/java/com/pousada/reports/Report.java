package com.pousada.reports;

public abstract class Report {
    protected final String lodgingName = "Pousada Maré Alta";

    public abstract String generate();

    protected String generateTitle(String reportName, String period) {
        return String.format("%s,%s,%s", lodgingName, reportName, period);
    }

    protected String generateCsvRow(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        return sb.toString();
    }
}
