package hotel.reports;

import java.util.List;

/**
 * The shape every report shares: a title line (inn, report name and period), the header, one line per row
 * and the total. Each report says only what is its own.
 */
public abstract class CsvReport<P, T> {

    private static final String INN = "Pousada Maré Alta";

    /** The report's name, as it appears in the title and in the URL. */
    public abstract String name();

    protected abstract String header();

    protected abstract List<T> rows(P period);

    protected abstract String line(T row);

    protected abstract String total(List<T> rows);

    public final String generate(P period) {
        List<T> rows = rows(period);
        StringBuilder csv = new StringBuilder();
        csv.append(INN).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (T row : rows) {
            csv.append(line(row)).append('\n');
        }
        csv.append(total(rows)).append('\n');
        return csv.toString();
    }
}
