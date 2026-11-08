package br.com.marealta.report;

import br.com.marealta.store.Tx;

/** Um relatório: novos relatórios são só mais uma implementação registrada em {@link Reports}. */
public interface Report {
    /** Nome usado na URL (/reports/{name}) e no título do CSV. */
    String name();

    /** Nome do parâmetro de consulta que traz o período (date, month...). */
    String param();

    /** Monta o relatório; {@code rawPeriod} é o valor cru do parâmetro, ainda não validado. */
    ReportTable build(Tx tx, String rawPeriod);
}
