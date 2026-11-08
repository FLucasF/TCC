package com.pousadamarealta.report;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseReport {
    protected static final String POUSADA_NAME = "Pousada Maré Alta";

    protected List<String> lines = new ArrayList<>();

    protected void addTitleLine(String reportName, String period) {
        lines.add(String.format("%s,%s,%s", POUSADA_NAME, reportName, period));
    }

    protected void addHeaderLine(String... columns) {
        lines.add(String.join(",", columns));
    }

    protected void addDataLine(String... values) {
        lines.add(String.join(",", values));
    }

    public String generate() {
        return String.join("\n", lines);
    }
}
