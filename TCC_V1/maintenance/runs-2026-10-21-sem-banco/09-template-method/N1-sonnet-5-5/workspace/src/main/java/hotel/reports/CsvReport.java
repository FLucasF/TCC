package hotel.reports;

import hotel.reservations.ReservationService;
import java.util.List;

/**
 * The shape every report shares: a title line (inn, report name, period), the header, the rows and the
 * total. Each report only says what is its own: its name, its header, its rows and its total.
 */
public abstract class CsvReport<P> {

    /** The columns of a report, its rows and its total line, each without the line break. */
    protected record Table(String header, List<String> rows, String total) {
    }

    /** The name in the title line and in the URL: /reports/{name}. */
    public abstract String name();

    protected abstract Table table(P period);

    public final String generate(P period) {
        Table table = table(period);
        StringBuilder csv = new StringBuilder();
        csv.append(ReservationService.HOTEL_NAME).append(',').append(name()).append(',').append(period).append('\n');
        csv.append(table.header()).append('\n');
        for (String row : table.rows()) {
            csv.append(row).append('\n');
        }
        csv.append(table.total()).append('\n');
        return csv.toString();
    }
}
