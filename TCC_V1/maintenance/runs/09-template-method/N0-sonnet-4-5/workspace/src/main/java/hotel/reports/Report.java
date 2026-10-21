package hotel.reports;

/** Base for all reports, handling common formatting (title line, future footer line). */
public abstract class Report {

    private static final String INN_NAME = "Pousada Maré Alta";

    protected String buildCsv(String reportName, String period, String headerAndRows) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',')
                .append(reportName).append(',')
                .append(period).append('\n');
        csv.append(headerAndRows);
        return csv.toString();
    }
}
