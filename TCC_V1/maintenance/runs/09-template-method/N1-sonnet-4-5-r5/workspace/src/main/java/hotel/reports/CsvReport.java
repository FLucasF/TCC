package hotel.reports;

/**
 * Formats a CSV report with a consistent structure: title line, header, data lines, and total line.
 * Future changes to the report format (like adding a date line) can be made here once for all reports.
 */
final class CsvReport {

    private final String reportName;
    private final String period;

    CsvReport(String reportName, String period) {
        this.reportName = reportName;
        this.period = period;
    }

    String format(String header, String dataLines, String totalLine) {
        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,").append(reportName).append(',').append(period).append('\n');
        csv.append(header).append('\n');
        csv.append(dataLines);
        csv.append(totalLine).append('\n');
        return csv.toString();
    }
}
