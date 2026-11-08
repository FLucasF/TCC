package hotel.reports;

import java.util.List;

/**
 * The shape every report (CSV) shares: a title line, the header, one line per item and the total.
 * A report only says what is its own: its name, its header, which items it lists and how it writes them.
 *
 * @param <P> the period the report covers (a night, a month)
 * @param <I> the items the report lists
 */
public abstract class Report<P, I> {

    private static final String INN_NAME = "Pousada Maré Alta";

    public final String generate(P period) {
        List<I> items = items(period);
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (I item : items) {
            csv.append(line(item)).append('\n');
        }
        csv.append(totalLine(items)).append('\n');
        return csv.toString();
    }

    /** The name of the report, as it appears in the title. */
    protected abstract String name();

    protected abstract String header();

    /** The items of the period, already in the order they are listed. */
    protected abstract List<I> items(P period);

    protected abstract String line(I item);

    protected abstract String totalLine(List<I> items);
}
