package hotel.reports;

import java.util.ArrayList;
import java.util.List;

/**
 * A CSV report with a title line, column headers, data rows and a summary row at the end.
 * When the structure of all reports changes (a new line, a new section), the change goes here.
 */
public final class CsvReport {

    private static final String INN_NAME = "Pousada Maré Alta";

    private final String reportName;
    private final String period;
    private final List<String> headers;
    private final List<String> rows;
    private String totalRow;

    public CsvReport(String reportName, String period, List<String> headers) {
        this.reportName = reportName;
        this.period = period;
        this.headers = new ArrayList<>(headers);
        this.rows = new ArrayList<>();
    }

    public void addRow(String... columns) {
        rows.add(String.join(",", columns));
    }

    public void setTotal(String... columns) {
        this.totalRow = String.join(",", columns);
    }

    public String render() {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName).append(',').append(period).append('\n');
        csv.append(String.join(",", headers)).append('\n');
        for (String row : rows) {
            csv.append(row).append('\n');
        }
        csv.append(totalRow).append('\n');
        return csv.toString();
    }
}
