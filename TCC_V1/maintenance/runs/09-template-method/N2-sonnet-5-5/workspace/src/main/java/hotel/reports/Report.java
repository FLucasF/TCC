package hotel.reports;

import java.util.List;

/**
 * The sequence every CSV report follows: title line, header, one line per row and the total.
 * Each report only says what varies: its name, header, rows and total.
 *
 * @param <P> the period the report covers (a night, a month)
 * @param <R> what each line of the report is about
 */
public abstract class Report<P, R> {

    static final String INN_NAME = "Pousada Maré Alta";

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

    /** The last line, with its own label. */
    protected abstract String total(List<R> rows);
}
