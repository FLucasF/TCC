package hotel.reports;

/**
 * The fixed sequence every report follows: title line, header, data lines, total.
 * Each report fills in the steps that vary.
 */
public abstract class ReportTemplate<T> {

    private static final String INN_NAME = "Pousada Maré Alta";

    public final String generate(T period) {
        StringBuilder csv = new StringBuilder();
        csv.append(titleLine(period));
        csv.append(headerLine());
        csv.append(dataLines(period));
        csv.append(totalLine(period));
        return csv.toString();
    }

    protected abstract String reportName();

    protected abstract String headerLine();

    protected abstract String dataLines(T period);

    protected abstract String totalLine(T period);

    private String titleLine(T period) {
        return INN_NAME + ',' + reportName() + ',' + period + '\n';
    }
}
