package hotel.reports;

import java.util.List;

/**
 * A CSV report. Every report is a title line, a header, the rows and a total line; each report only says what
 * its name, header, rows and total are, so a change in the shared layout is made here, once.
 */
public abstract class Report<P> {

    static final String INN_NAME = "Pousada Maré Alta";

    /** The rows of a report and its total line, as lists of cells. */
    protected record Table(List<List<String>> rows, List<String> total) {}

    public final String generate(P period) {
        Table table = table(period);
        StringBuilder csv = new StringBuilder();
        line(csv, List.of(INN_NAME, name(), period.toString()));
        line(csv, header());
        for (List<String> row : table.rows()) {
            line(csv, row);
        }
        line(csv, table.total());
        return csv.toString();
    }

    /** The name of the report, as it appears in the title line. */
    protected abstract String name();

    protected abstract List<String> header();

    protected abstract Table table(P period);

    private static void line(StringBuilder csv, List<String> cells) {
        csv.append(String.join(",", cells)).append('\n');
    }
}
