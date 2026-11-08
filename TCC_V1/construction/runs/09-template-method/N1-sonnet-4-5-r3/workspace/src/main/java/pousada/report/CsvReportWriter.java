package pousada.report;

import java.util.stream.Collectors;

public class CsvReportWriter {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public String write(Report report) {
        StringBuilder csv = new StringBuilder();

        csv.append(formatTitleLine(report));
        csv.append(formatHeaderLine(report));

        for (Report.Row row : report.getDataRows()) {
            csv.append(formatDataLine(row));
        }

        csv.append(formatDataLine(report.getTotalRow()));

        return csv.toString();
    }

    private String formatTitleLine(Report report) {
        return String.format("%s,%s,%s\n", POUSADA_NAME, report.getName(), report.getPeriod());
    }

    private String formatHeaderLine(Report report) {
        return report.getColumns().stream()
            .collect(Collectors.joining(",", "", "\n"));
    }

    private String formatDataLine(Report.Row row) {
        return row.values().stream()
            .collect(Collectors.joining(",", "", "\n"));
    }
}
