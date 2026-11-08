package hotel.reports;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Common formatting for all reports: title line, then the main content.
 * Future changes (like a generation date footer) go here and apply to every report.
 */
public final class ReportFormatter {

    private static final String INN_NAME = "Pousada Maré Alta";

    private ReportFormatter() {}

    public static String withTitle(String reportName, LocalDate date, String content) {
        return title(reportName, date.toString()) + content;
    }

    public static String withTitle(String reportName, YearMonth month, String content) {
        return title(reportName, month.toString()) + content;
    }

    private static String title(String reportName, String period) {
        return INN_NAME + ',' + reportName + ',' + period + '\n';
    }
}
