package br.com.mare.report;

import java.util.List;

/**
 * Esqueleto de todo relatório CSV: título, cabeçalho, linhas e total.
 * Mudar o jeito dos relatórios (ex.: linha final com a data de emissão) é editar só {@link #render}.
 *
 * @param <P> o período que o relatório recebe (uma noite, um mês)
 */
public abstract class Report<P> {
    static final String HOUSE = "Pousada Maré Alta";

    /** Linhas do corpo e a linha de total, já formatadas. */
    protected record Body(List<String> rows, String total) {
    }

    /** Nome usado no título e na rota. */
    public abstract String name();

    /** Nome do parâmetro de consulta (date, month). */
    public abstract String parameter();

    /** Converte o texto recebido; lança INVALID_REQUEST se estiver fora do formato. */
    protected abstract P parse(String raw);

    protected abstract String periodLabel(P period);

    protected abstract String header();

    protected abstract Body body(P period);

    public final String render(String rawPeriod) {
        P period = parse(rawPeriod);
        Body body = body(period);
        StringBuilder csv = new StringBuilder();
        line(csv, HOUSE + "," + name() + "," + periodLabel(period));
        line(csv, header());
        body.rows().forEach(row -> line(csv, row));
        line(csv, body.total());
        return csv.toString();
    }

    private static void line(StringBuilder csv, String line) {
        csv.append(line).append('\n');
    }
}
