package hotel.reports;

/** Base class for CSV reports with title, headers, data, and total. */
abstract class Report {

    private static final String POUSADA_NAME = "Pousada Maré Alta";

    protected final String buildCsv(String reportName, String period, StringBuilder content) {
        StringBuilder csv = new StringBuilder();
        csv.append(POUSADA_NAME).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(content);
        return csv.toString();
    }
}
