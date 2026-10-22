package hotel.reports;

import java.util.List;

/**
 * The shape every CSV report shares: a title line, the header, one line per row and the total.
 * Each report only says what is its own; anything that must change in all of them changes here.
 *
 * @param <P> the period the report is about (a night, a month)
 * @param <R> what one row of the report is made from
 */
public abstract class Report<P, R> {

    private static final String INN_NAME = "Pousada Maré Alta";

    public final String generate(P period) {
        List<R> rows = rows(period);
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (R row : rows) {
            csv.append(line(row)).append('\n');
        }
        csv.append(total(rows)).append('\n');
        return csv.toString();
    }

    /** The report's name, as it appears in the title line. */
    protected abstract String name();

    protected abstract String header();

    /** The rows of the period, already in the order they are listed. */
    protected abstract List<R> rows(P period);

    protected abstract String line(R row);

    /** The last line, total included. */
    protected abstract String total(List<R> rows);
}
