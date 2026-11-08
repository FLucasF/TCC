package hotel.reports;

/**
 * Template for CSV reports: all follow the same structure (title, header, data rows, total row), but
 * each one varies in columns, filtering, and total format.
 */
abstract class Report {

    /**
     * The template method: the sequence of steps that every report follows. Subclasses must not
     * override this; they fill in the varying steps instead.
     */
    protected final String generate(String period) {
        StringBuilder csv = new StringBuilder();
        csv.append(title(period));
        csv.append(header());
        csv.append(dataRows());
        csv.append(totalRow());
        return csv.toString();
    }

    private String title(String period) {
        return "Pousada Maré Alta," + reportName() + "," + period + "\n";
    }

    /** The name of this report, as it appears in the title line (e.g., "occupancy", "revenue"). */
    protected abstract String reportName();

    /** The CSV header: column names, ending with a newline. */
    protected abstract String header();

    /** The data rows: one line per item, each ending with a newline. */
    protected abstract String dataRows();

    /** The total row: starts with "total," and the aggregated values, ending with a newline. */
    protected abstract String totalRow();
}
