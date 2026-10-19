package br.com.marealta.reports;

/** Um relatório CSV exposto em GET /reports/{name}?{paramName}=valor. */
public interface Report {
    String name();

    /** Nome do parâmetro de período na URL (date, month...). */
    String paramName();

    /** Gera o CSV; lança INVALID_REQUEST se o período faltar ou estiver fora do formato. */
    String render(String periodValue);
}
