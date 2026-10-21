package br.com.marealta.reservas.reports;

/** Um relatório com nome (usado na rota e no título) e um parâmetro de período. */
public interface Report {
    /** Nome do relatório: aparece na URL /reports/{name} e na linha de título. */
    String name();

    /** Nome do parâmetro de consulta que traz o período (date, month, ...). */
    String periodParam();

    /** @throws br.com.marealta.reservas.domain.BookingException INVALID_REQUEST se o período for inválido */
    ReportTable build(String period);
}
