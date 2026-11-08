package com.pousada.report;

import java.util.List;

/**
 * Todo relatório CSV segue a mesma sequência: título, cabeçalho, linhas e total.
 * Essa sequência fica aqui, uma vez só; cada relatório informa apenas o que é dele.
 */
public abstract class Report<P> {
    public static final String HOUSE_NAME = "Pousada Maré Alta";

    /** Linhas de dados e a linha de total. */
    public record Body(List<List<String>> rows, List<String> total) {
    }

    /** Nome usado no título e na URL. */
    public abstract String name();

    /** Nome do parâmetro de consulta que traz o período. */
    public abstract String parameter();

    protected abstract P parsePeriod(String raw);

    protected abstract String periodLabel(P period);

    protected abstract List<String> header();

    protected abstract Body body(P period);

    public final String render(String rawPeriod) {
        P period = parsePeriod(rawPeriod);
        Body body = body(period);
        var csv = new StringBuilder();
        line(csv, List.of(HOUSE_NAME, name(), periodLabel(period)));
        line(csv, header());
        body.rows().forEach(row -> line(csv, row));
        line(csv, body.total());
        return csv.toString();
    }

    private static void line(StringBuilder csv, List<String> cells) {
        csv.append(String.join(",", cells)).append('\n');
    }
}
