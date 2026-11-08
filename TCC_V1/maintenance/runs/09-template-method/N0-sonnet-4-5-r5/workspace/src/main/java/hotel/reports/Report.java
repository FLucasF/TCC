package hotel.reports;

/**
 * Shared formatting for the inn's CSV reports.
 * All reports start with a title line and may have a footer added in the future.
 */
public final class Report {

    private static final String INN_NAME = "Pousada Maré Alta";

    public static String withTitle(String reportName, String period, String content) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(content);
        return csv.toString();
    }
}
