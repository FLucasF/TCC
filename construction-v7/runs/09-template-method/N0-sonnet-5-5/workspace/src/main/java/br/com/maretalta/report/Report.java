package br.com.maretalta.report;

/** A CSV report served at GET /reports/{name}?{paramName}=... */
public interface Report {
    String name();

    /** Name of the query parameter that selects the period (e.g. "date", "month"). */
    String paramName();

    /** @param rawParam raw value of the period parameter, null if absent; throws ApiException (400) when invalid */
    String generate(String rawParam);
}
