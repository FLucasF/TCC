package hotel.reports;

/**
 * A CSV report for one period (a night, a month...). Every report starts with a title line
 * (inn, report name, period); the subclass writes the rest.
 */
public abstract class Report<P> {

    private static final String INN = "Pousada Maré Alta";

    /** The name that goes in the title line and in the URL: /reports/{name}. */
    public abstract String name();

    protected abstract String body(P period);

    public final String generate(P period) {
        return INN + "," + name() + "," + period + "\n" + body(period);
    }
}
