package hotel.reports;

import java.util.function.Consumer;

/**
 * The common structure of all CSV reports: title line, headers and rows, total line.
 * This centralizes the format so changes (like adding a generation date footer) apply to all reports at once.
 */
public final class ReportFormat {

    private static final String INN_NAME = "Pousada Maré Alta";

    private final String reportName;
    private final String period;
    private final StringBuilder csv;

    public ReportFormat(String reportName, String period) {
        this.reportName = reportName;
        this.period = period;
        this.csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName).append(',').append(period).append('\n');
    }

    public void appendHeader(String header) {
        csv.append(header).append('\n');
    }

    public void appendRows(Consumer<StringBuilder> rowsBuilder) {
        rowsBuilder.accept(csv);
    }

    public void appendTotal(String totalLine) {
        csv.append(totalLine).append('\n');
    }

    public String build() {
        return csv.toString();
    }
}
