package pousada.report;

public interface Report {
    /** O nome que aparece no título e na URL: /reports/{name}. */
    String name();

    PeriodKind periodKind();

    ReportTable build(Period period, ReportSource source);
}
