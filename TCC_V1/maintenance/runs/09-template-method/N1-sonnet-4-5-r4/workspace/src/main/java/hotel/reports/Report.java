package hotel.reports;

/**
 * Base for all CSV reports. Every report starts with a title line (inn name, report name, period),
 * then a header, data lines, and a total line. Future changes to this format (such as adding a
 * generation date) apply to all reports at once.
 */
public abstract class Report {

    private static final String INN_NAME = "Pousada Maré Alta";

    protected abstract String reportName();

    protected abstract String period();

    protected abstract String header();

    protected abstract String dataLines();

    protected abstract String totalLine();

    public String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName()).append(',').append(period()).append('\n');
        csv.append(header()).append('\n');
        csv.append(dataLines());
        csv.append(totalLine()).append('\n');
        return csv.toString();
    }
}
