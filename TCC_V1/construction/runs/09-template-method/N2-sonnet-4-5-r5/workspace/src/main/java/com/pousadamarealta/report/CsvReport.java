package com.pousadamarealta.report;

import java.util.ArrayList;
import java.util.List;

public abstract class CsvReport {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public final String generate() {
        List<String> lines = new ArrayList<>();
        lines.add(buildTitleLine());
        lines.add(buildHeaderLine());
        lines.addAll(buildDataLines());
        lines.add(buildTotalLine());
        return String.join("\n", lines);
    }

    private String buildTitleLine() {
        return POUSADA_NAME + "," + getReportName() + "," + getPeriod();
    }

    protected abstract String getReportName();

    protected abstract String getPeriod();

    protected abstract String buildHeaderLine();

    protected abstract List<String> buildDataLines();

    protected abstract String buildTotalLine();
}
