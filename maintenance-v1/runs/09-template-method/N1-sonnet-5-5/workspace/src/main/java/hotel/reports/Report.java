package hotel.reports;

/** A CSV report the HTTP API serves at /reports/{name}, for a period given in one query parameter. */
public interface Report {

    String name();

    /** The query parameter that carries the period, e.g. "date" or "month". */
    String periodParameter();

    /** The report for the period as the API received it; throws INVALID_REQUEST if it cannot be read. */
    String generateFor(String period);
}
