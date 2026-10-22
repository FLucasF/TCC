package hotel.reports;

/** Template for CSV reports: title, headers, rows, and total. */
public abstract class Report {

    protected final String title(String name, String period) {
        return "Pousada Maré Alta," + name + "," + period + "\n";
    }

    protected abstract String headers();

    protected abstract String rows();

    protected abstract String total();

    protected final String build() {
        return title(reportName(), period()) + headers() + rows() + total();
    }

    protected abstract String reportName();

    protected abstract String period();
}
