package com.pousada.report;

import java.util.ArrayList;
import java.util.List;

public abstract class Report {
    protected String title;
    protected List<String> dataLines;

    public String generate() {
        title = buildTitle();
        String header = buildHeader();
        dataLines = buildDataLines();
        String total = buildTotalLine();

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append(header).append("\n");
        for (String line : dataLines) {
            sb.append(line).append("\n");
        }
        sb.append(total);

        return sb.toString();
    }

    protected abstract String buildTitle();
    protected abstract String buildHeader();
    protected abstract List<String> buildDataLines();
    protected abstract String buildTotalLine();

    protected List<String> dataLines() {
        return new ArrayList<>();
    }
}
