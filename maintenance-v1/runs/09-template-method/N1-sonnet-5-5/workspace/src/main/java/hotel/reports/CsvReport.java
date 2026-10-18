package hotel.reports;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import java.util.function.Function;

/** The shape every report shares: a title line, then the header, the rows and the total. */
abstract class CsvReport<P> implements Report {

    static final String INN_NAME = "Pousada Maré Alta";

    private final String name;
    private final PeriodKind<P> periodKind;

    CsvReport(String name, PeriodKind<P> periodKind) {
        this.name = name;
        this.periodKind = periodKind;
    }

    /** The header, the rows and the total, each line ending in a line break. */
    abstract String body(P period);

    public String generate(P period) {
        return INN_NAME + ',' + name + ',' + period + '\n' + body(period);
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final String periodParameter() {
        return periodKind.parameter();
    }

    @Override
    public final String generateFor(String period) {
        return generate(periodKind.parse(period));
    }

    /** What a report's period is: a name for the query parameter, and how to read it. */
    record PeriodKind<P>(String parameter, String format, Function<String, P> parser) {

        P parse(String text) {
            try {
                return parser.apply(text);
            } catch (java.time.format.DateTimeParseException badPeriod) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "'" + text + "' is not a " + parameter + " (" + format + ")");
            }
        }
    }
}
