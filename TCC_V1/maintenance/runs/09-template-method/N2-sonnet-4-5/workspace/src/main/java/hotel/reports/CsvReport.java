package hotel.reports;

/**
 * Template for CSV reports: all reports follow the same sequence (title, header, data rows, total),
 * and each one fills in its own columns and data.
 */
abstract class CsvReport {

    private static final String INN_NAME = "Pousada Maré Alta";

    protected abstract String reportName();

    protected abstract String period();

    protected abstract String header();

    protected abstract String dataRows();

    protected abstract String totalRow();

    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName()).append(',').append(period()).append('\n');
        csv.append(header()).append('\n');
        csv.append(dataRows());
        csv.append(totalRow()).append('\n');
        return csv.toString();
    }
}
