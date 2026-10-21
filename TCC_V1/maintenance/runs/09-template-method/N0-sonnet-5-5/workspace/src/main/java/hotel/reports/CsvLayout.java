package hotel.reports;

import java.util.List;

/**
 * The one place that decides how every report is laid out: a title line (inn, report name, period),
 * the header, the rows and the total. A change to the shape of all reports belongs here.
 */
final class CsvLayout {

    static final String INN_NAME = "Pousada Maré Alta";

    private CsvLayout() {
    }

    static String render(String reportName, String period, String header, List<String> rows, String totalLine) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(header).append('\n');
        for (String row : rows) {
            csv.append(row).append('\n');
        }
        csv.append(totalLine).append('\n');
        return csv.toString();
    }
}
