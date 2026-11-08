package br.com.marealta.report;

import java.sql.Connection;
import java.sql.SQLException;

/** A report. To add one: implement this and register it in {@link ReportCatalog}. */
public interface Report {

    /** Name used in the URL (/reports/{name}) and in the title line. */
    String name();

    PeriodKind periodKind();

    /** Builds the report; {@code period} was already validated and normalized. */
    ReportTable build(Connection connection, String period) throws SQLException;
}
