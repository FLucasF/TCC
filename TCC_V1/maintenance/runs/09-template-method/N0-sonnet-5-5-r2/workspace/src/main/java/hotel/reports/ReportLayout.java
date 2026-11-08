package hotel.reports;

import java.util.List;

/**
 * How every CSV report is laid out: a title line (inn, report, period), the column header, the rows and
 * the total line. A change in the look of the reports is made here, once.
 */
final class ReportLayout {

    private static final String INN = "Pousada Maré Alta";

    private ReportLayout() {
    }

    static String render(String reportName, Object period, String header, List<String> rows, String total) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(header).append('\n');
        for (String row : rows) {
            csv.append(row).append('\n');
        }
        csv.append(total).append('\n');
        return csv.toString();
    }
}
