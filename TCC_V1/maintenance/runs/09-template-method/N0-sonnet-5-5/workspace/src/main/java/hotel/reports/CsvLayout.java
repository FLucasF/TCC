package hotel.reports;

import java.util.List;

/**
 * The shape shared by every report: a title line (inn, report name, period), the header, the rows and
 * the total. Anything that must change in all the reports at once (like a closing line) changes here.
 */
final class CsvLayout {

    static final String INN_NAME = "Pousada Maré Alta";

    private CsvLayout() {
    }

    static String render(String reportName, String period, String header, List<String> rows, String total) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(header).append('\n');
        for (String row : rows) {
            csv.append(row).append('\n');
        }
        csv.append(total).append('\n');
        return csv.toString();
    }
}
