package hotel.reports;

/**
 * Template for all CSV reports: title, header, rows, total.
 * Each report fills in its own name, columns and data.
 */
public abstract class Report {

    private static final String INN_NAME = "Pousada Maré Alta";

    /** The template method: the sequence of a report. */
    public final String generate(String period) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        csv.append(rows());
        csv.append(totalLine()).append('\n');
        return csv.toString();
    }

    /** The name that appears in the title line. */
    protected abstract String reportName();

    /** The column names, separated by commas. */
    protected abstract String header();

    /** The data rows, each ending with a newline. */
    protected abstract String rows();

    /** The last line, starting with "total,". */
    protected abstract String totalLine();
}
