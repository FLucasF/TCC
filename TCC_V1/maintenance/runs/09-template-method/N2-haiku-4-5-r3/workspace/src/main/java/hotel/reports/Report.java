package hotel.reports;

import java.util.List;

/** Base class for CSV reports using the Template Method pattern. */
public abstract class Report<T> {

    protected static final String HOTEL_NAME = "Pousada Maré Alta";

    public final String generate(T period) {
        StringBuilder csv = new StringBuilder();
        csv.append(formatTitle(period)).append("\n");
        csv.append(formatHeader()).append("\n");
        for (String line : formatLines(period)) {
            csv.append(line).append("\n");
        }
        csv.append(formatTotal(period)).append("\n");
        return csv.toString();
    }

    protected abstract String reportName();

    protected abstract String formatTitle(T period);

    protected abstract String formatHeader();

    protected abstract List<String> formatLines(T period);

    protected abstract String formatTotal(T period);

    protected String titleLine(T period) {
        return HOTEL_NAME + "," + reportName() + "," + period;
    }
}
