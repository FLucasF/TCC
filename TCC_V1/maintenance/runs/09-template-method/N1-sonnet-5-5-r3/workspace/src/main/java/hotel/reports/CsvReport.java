package hotel.reports;

import java.util.List;

/**
 * The shape every report shares: a title line (inn, report name and period), the header, one line per
 * row and the total. A report only says what its header, rows and total are.
 *
 * @param <P> what the report is about: a night, a month
 * @param <R> what each line of the report comes from
 */
abstract class CsvReport<P, R> {

    private static final String INN = "Pousada Maré Alta";

    private final String name;

    CsvReport(String name) {
        this.name = name;
    }

    public final String generate(P period) {
        List<R> rows = rows(period);
        StringBuilder csv = new StringBuilder();
        csv.append(INN).append(',').append(name).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (R row : rows) {
            csv.append(line(row)).append('\n');
        }
        csv.append(total(rows)).append('\n');
        return csv.toString();
    }

    /** The columns, separated by commas. */
    protected abstract String header();

    /** The rows of the period, in the order they are listed. */
    protected abstract List<R> rows(P period);

    protected abstract String line(R row);

    /** The last line, given the rows listed. */
    protected abstract String total(List<R> rows);
}
