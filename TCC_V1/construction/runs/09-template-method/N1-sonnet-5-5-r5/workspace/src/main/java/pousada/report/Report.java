package pousada.report;

/** Um relatório: só diz o seu nome, o parâmetro de período e o conteúdo. O formato é de {@link CsvReportWriter}. */
public interface Report {
    /** Nome no título e no endereço: GET /reports/{name}. */
    String name();

    /** Nome do parâmetro de consulta que traz o período. */
    String parameter();

    /** Gera o relatório do período, que chega como texto; formato inválido é 400. */
    ReportContent generate(String period);
}
