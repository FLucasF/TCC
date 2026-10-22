package hotel.reports;

import java.util.List;

/**
 * The shape every CSV report shares: a title line (inn, report name, period), the header, the lines
 * (rows, then the total). Each report supplies only its name, header and lines.
 *
 * @param <P> what the report is asked about: a night, a month
 */
abstract class CsvReport<P> {

    private static final String INN = "Pousada Maré Alta";

    private final String name;

    CsvReport(String name) {
        this.name = name;
    }

    public final String generate(P period) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN).append(',').append(name).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (String line : lines(period)) {
            csv.append(line).append('\n');
        }
        return csv.toString();
    }

    /** The column names, comma separated. */
    abstract String header();

    /** The rows of the report, followed by its total line. */
    abstract List<String> lines(P period);
}
