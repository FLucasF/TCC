package hotel.reports;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * The shape every report shares (CSV): a title line with the inn, the report and the period, then the
 * lines of the report itself (header, rows and total). A change to what all reports have goes here.
 */
public abstract class CsvReport<P> {

    private static final String INN = "Pousada Maré Alta";

    /** The report's name: its title and its place in {@code /reports/<name>}. */
    public abstract String name();

    /** The query parameter that carries the period. */
    public abstract String periodParam();

    public abstract P parsePeriod(String text);

    /** The header, the rows and the total, one string per line. */
    protected abstract List<String> lines(P period);

    public final String generate(P period) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN).append(',').append(name()).append(',').append(period).append('\n');
        for (String line : lines(period)) {
            csv.append(line).append('\n');
        }
        return csv.toString();
    }

    public final String generate(String periodText) {
        return generate(parsePeriod(periodText));
    }

    protected static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException badDate) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "'" + text + "' is not a date (yyyy-MM-dd)");
        }
    }

    protected static YearMonth parseMonth(String text) {
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException badMonth) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "'" + text + "' is not a month (yyyy-MM)");
        }
    }
}
