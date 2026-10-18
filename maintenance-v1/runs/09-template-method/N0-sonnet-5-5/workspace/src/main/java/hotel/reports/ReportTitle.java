package hotel.reports;

/** The first line of every report: the inn, the report and the period it covers. */
final class ReportTitle {

    private static final String INN = "Pousada Maré Alta";

    private ReportTitle() {
    }

    static String line(String report, Object period) {
        return INN + "," + report + "," + period + "\n";
    }
}
