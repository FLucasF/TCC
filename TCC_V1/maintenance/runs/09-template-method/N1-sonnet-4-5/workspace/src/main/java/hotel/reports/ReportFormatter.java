package hotel.reports;

/** Formats common parts of CSV reports. */
public final class ReportFormatter {

    private static final String INN_NAME = "Pousada Maré Alta";

    private ReportFormatter() {}

    public static String withTitle(String reportName, String period, String content) {
        return INN_NAME + "," + reportName + "," + period + "\n" + content;
    }
}
