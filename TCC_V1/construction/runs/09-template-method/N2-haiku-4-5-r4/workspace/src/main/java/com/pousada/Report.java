package com.pousada;

import java.util.List;

public abstract class Report {
    protected String reportName;
    protected String period;

    public Report(String reportName, String period) {
        this.reportName = reportName;
        this.period = period;
    }

    public String generate() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTitle()).append("\n");
        sb.append(getHeader()).append("\n");

        var lines = getLines();
        for (String line : lines) {
            sb.append(line).append("\n");
        }

        sb.append(getTotal());
        return sb.toString();
    }

    private String getTitle() {
        return "Pousada Maré Alta," + reportName + "," + period;
    }

    protected abstract String getHeader();
    protected abstract List<String> getLines();
    protected abstract String getTotal();
}
