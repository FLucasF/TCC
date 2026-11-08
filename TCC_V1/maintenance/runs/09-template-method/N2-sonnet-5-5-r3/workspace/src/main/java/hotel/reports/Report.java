package hotel.reports;

import java.util.List;

/**
 * A CSV report: a title line, the header, the lines and the total. Every report follows this sequence;
 * each one gives only its name, its header and its lines.
 */
public abstract class Report<P> {

    private static final String INN_NAME = "Pousada Maré Alta";

    /** The name in the title line, also the path of the report in the API (/reports/{name}). */
    public abstract String name();

    protected abstract String header();

    /** The lines after the header, the total last; each one without the line break. */
    protected abstract List<String> lines(P period);

    public final String generate(P period) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (String line : lines(period)) {
            csv.append(line).append('\n');
        }
        return csv.toString();
    }
}
