package hotel.reports;

import java.util.List;

/**
 * Base class for all CSV reports. The structure is fixed: title line, header, data rows, total line.
 * Each concrete report defines its own name, columns, data and total.
 */
public abstract class Report<T> {

    /**
     * Generates the full CSV report with title, header, data rows and total.
     */
    public final String generateCsv(Object period) {
        List<T> data = collectData(period);

        StringBuilder csv = new StringBuilder();
        csv.append(titleLine(period));
        csv.append(headerLine());
        for (T row : data) {
            csv.append(dataLine(row));
        }
        csv.append(totalLine(data));
        return csv.toString();
    }

    /** The name of this report for the title line (e.g., "occupancy", "revenue"). */
    protected abstract String reportName();

    /** The data rows to include in this report for the given period. */
    protected abstract List<T> collectData(Object period);

    /** The CSV header line (e.g., "room,type,reservation\n"). */
    protected abstract String headerLine();

    /** One CSV data line for the given row (e.g., "101,STANDARD,5\n"). */
    protected abstract String dataLine(T row);

    /** The total line at the end of the report (e.g., "total,10\n"). */
    protected abstract String totalLine(List<T> data);

    private String titleLine(Object period) {
        return "Pousada Maré Alta," + reportName() + "," + period + "\n";
    }
}
