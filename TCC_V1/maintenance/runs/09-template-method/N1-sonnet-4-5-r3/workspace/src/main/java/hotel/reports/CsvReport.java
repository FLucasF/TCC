package hotel.reports;

/**
 * Common formatting for CSV reports.
 * When the report format changes (title line today, generation date tomorrow),
 * the change happens here once and applies to all reports.
 */
final class CsvReport {

    private static final String INN_NAME = "Pousada Maré Alta";

    static String withTitle(String reportName, String period, String content) {
        return INN_NAME + "," + reportName + "," + period + "\n" + content;
    }

    private CsvReport() {}
}
