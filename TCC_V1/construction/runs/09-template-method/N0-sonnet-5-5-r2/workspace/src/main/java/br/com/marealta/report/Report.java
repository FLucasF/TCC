package br.com.marealta.report;

import br.com.marealta.service.ApiException;

/**
 * Um relatório. Para criar um novo basta implementar esta interface e registrá-la em
 * {@link Reports#all}; título, CSV e rota são iguais para todos.
 */
public interface Report {
    /** Nome usado na rota (/reports/{name}) e no título. */
    String name();

    /** Nome do parâmetro de consulta que traz o período. */
    String parameter();

    /** @throws ApiException 400 se o período for inválido */
    ReportData generate(String period);
}
