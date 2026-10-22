package com.pousada.service.report;

import java.time.LocalDate;
import java.time.YearMonth;
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

        sb.append(getHeader()).append("\n");
        sb.append(getColumnHeader()).append("\n");

        List<String> rows = getDataRows();
        for (String row : rows) {
            sb.append(row).append("\n");
        }

        sb.append(getTotalLine());

        return sb.toString();
    }

    private String getHeader() {
        return "Pousada Maré Alta," + reportName + "," + period;
    }

    protected abstract String getColumnHeader();

    protected abstract List<String> getDataRows();

    protected abstract String getTotalLine();
}
