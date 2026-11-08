package br.com.marealta.report;

import br.com.marealta.store.Tx;

/** A report that can be requested as GET /reports/{name}?{parameter}=... */
public interface Report {
    /** Name used in the route and in the title line. */
    String name();

    /** Name of the query parameter holding the period. */
    String parameter();

    /** Builds the report; throws INVALID_REQUEST if the period is malformed. */
    ReportTable generate(String period, Tx tx);
}
