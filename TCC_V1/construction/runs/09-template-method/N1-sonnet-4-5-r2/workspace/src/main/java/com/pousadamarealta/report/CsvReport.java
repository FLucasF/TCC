package com.pousadamarealta.report;

import java.util.List;

public abstract class CsvReport {
    protected abstract String getReportName();
    protected abstract String getPeriod();
    protected abstract List<String> getColumnNames();
    protected abstract List<List<String>> getDataRows();
    protected abstract List<String> getTotalRow();

    public String generate() {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,").append(getReportName()).append(",").append(getPeriod()).append("\n");

        csv.append(String.join(",", getColumnNames())).append("\n");

        for (List<String> row : getDataRows()) {
            csv.append(String.join(",", row)).append("\n");
        }

        csv.append(String.join(",", getTotalRow()));

        return csv.toString();
    }
}
