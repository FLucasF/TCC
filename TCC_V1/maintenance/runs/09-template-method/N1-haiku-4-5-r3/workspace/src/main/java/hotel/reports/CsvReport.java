package hotel.reports;

public abstract class CsvReport {
    protected static final String INN_NAME = "Pousada Maré Alta";

    protected final String formatCsv(String reportName, String period, String columnHeaders, String rows, String total) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(",").append(reportName).append(",").append(period).append("\n");
        csv.append(columnHeaders).append("\n");
        csv.append(rows);
        csv.append(total);
        return csv.toString();
    }
}
