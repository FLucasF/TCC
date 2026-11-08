package hotel.reports;

/**
 * Base for all CSV reports: the title, headers, data lines, and total, in that order.
 * When the format changes, every report that extends this gets the change at once.
 */
abstract class CsvReport {

    protected abstract String reportName();

    protected abstract String headers();

    protected abstract String dataLines();

    protected abstract String totalLine();

    protected final String titleLine(String period) {
        return "Pousada Maré Alta," + reportName() + "," + period + "\n";
    }

    protected final String assemble(String period) {
        StringBuilder csv = new StringBuilder();
        csv.append(titleLine(period));
        csv.append(headers());
        csv.append(dataLines());
        csv.append(totalLine());
        return csv.toString();
    }
}
