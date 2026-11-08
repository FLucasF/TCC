package hotel.reports;

import java.util.List;
import java.util.StringJoiner;

/**
 * What every CSV report has in common: a title line (inn, report name and period), then the header, the rows
 * and the total line. A report only says what its table holds; the shape of the file is written here, once.
 */
public abstract class CsvReport<P> {

    private static final String INN = "Pousada Maré Alta";

    /** The report's name, as it appears in the title line and in the URL. */
    public abstract String name();

    protected abstract Table table(P period);

    public final String generate(P period) {
        Table table = table(period);
        StringBuilder csv = new StringBuilder();
        csv.append(line(INN, name(), period)).append('\n');
        csv.append(table.header()).append('\n');
        for (String row : table.rows()) {
            csv.append(row).append('\n');
        }
        csv.append(table.total()).append('\n');
        return csv.toString();
    }

    protected static String line(Object... cells) {
        StringJoiner line = new StringJoiner(",");
        for (Object cell : cells) {
            line.add(String.valueOf(cell));
        }
        return line.toString();
    }

    /** The header line, the data lines and the total line, each without the line break. */
    protected record Table(String header, List<String> rows, String total) {
    }
}
