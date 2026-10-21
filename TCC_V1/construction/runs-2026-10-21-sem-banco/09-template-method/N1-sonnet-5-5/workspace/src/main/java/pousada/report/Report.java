package pousada.report;

public interface Report {
    String name();

    /** Nome do parâmetro de consulta que traz o período (date, month...). */
    String parameter();

    ReportData generate(String period);
}
