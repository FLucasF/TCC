package hotel.reports;

import java.util.List;

/**
 * The shape shared by every report (CSV): a title line with the inn's name, the report's name and its
 * period, then the header, the rows and the total. Anything that must change in all the reports at once
 * is changed here.
 */
abstract class CsvReport<P> {

    private static final String INN_NAME = "Pousada Maré Alta";

    /** What a report puts between its header and its end. */
    protected record Body(List<String> rows, String total) {
    }

    /** The name in the title line, which is also the report's name in the API. */
    protected abstract String name();

    protected abstract String header();

    protected abstract Body body(P period);

    public final String generate(P period) {
        Body body = body(period);
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(header()).append('\n');
        for (String row : body.rows()) {
            csv.append(row).append('\n');
        }
        csv.append(body.total()).append('\n');
        return csv.toString();
    }

    protected static String line(Object... fields) {
        StringBuilder line = new StringBuilder();
        for (Object field : fields) {
            if (line.length() > 0) {
                line.append(',');
            }
            line.append(field);
        }
        return line.toString();
    }
}
