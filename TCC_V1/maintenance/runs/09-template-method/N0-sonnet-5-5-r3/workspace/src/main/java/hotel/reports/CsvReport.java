package hotel.reports;

import java.util.List;

/**
 * The layout every report shares: a title line (inn, report name, period), the header, the rows and the
 * closing line. Anything that must change in all the reports at once changes here.
 */
final class CsvReport {

    private static final String INN = "Pousada Maré Alta";

    private CsvReport() {
    }

    static String render(String reportName, String period, String header, List<String> rows, String closing) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(header).append('\n');
        for (String row : rows) {
            csv.append(row).append('\n');
        }
        csv.append(closing).append('\n');
        return csv.toString();
    }
}
