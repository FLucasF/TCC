package hotel.reports;

/**
 * Common formatting for all CSV reports. When the format changes (e.g., adding a footer with the
 * generation date), updating this class changes all reports at once.
 */
final class ReportFormat {

    private static final String INN_NAME = "Pousada Maré Alta";

    static String titleLine(String reportName, String period) {
        return INN_NAME + ',' + reportName + ',' + period + '\n';
    }
}
