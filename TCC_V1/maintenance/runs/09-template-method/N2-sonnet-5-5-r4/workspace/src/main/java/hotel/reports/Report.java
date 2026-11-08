package hotel.reports;

import hotel.reservations.Reservation;
import java.util.Comparator;
import java.util.List;

/**
 * The shape every CSV report shares: a title line, the header, the rows and the total. Each report fills in only
 * its name, its header and its content; a change to the shape of all reports is an edit to {@link #generate}.
 *
 * @param <P> what the report covers: a night, a month
 */
public abstract class Report<P> {

    static final String INN_NAME = "Pousada Maré Alta";

    protected static final Comparator<Reservation> BY_CHECK_IN_THEN_ID =
            Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id);

    /** The rows of a report, already formatted, and its last line. */
    protected record Content(List<String> rows, String total) {}

    public final String generate(P period) {
        Content content = content(period);
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (String row : content.rows()) {
            csv.append(row).append('\n');
        }
        csv.append(content.total()).append('\n');
        return csv.toString();
    }

    /** The name of the report, as it appears in the title line. */
    protected abstract String name();

    protected abstract String header();

    protected abstract Content content(P period);
}
