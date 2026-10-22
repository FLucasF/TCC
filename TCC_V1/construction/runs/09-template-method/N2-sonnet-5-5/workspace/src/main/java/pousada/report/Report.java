package pousada.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequência única de todo relatório CSV: título, cabeçalho, linhas e total.
 * Cada relatório só diz o seu nome, o parâmetro que recebe e o conteúdo da tabela.
 * Mudar o jeito dos relatórios (ex.: uma linha final com a data) é mudar apenas {@link #generate}.
 */
public abstract class Report<P> {
    static final String PROPERTY_NAME = "Pousada Maré Alta";

    public record Table(String header, List<String> rows, String total) {}

    /** Nome usado na URL e no título (occupancy, revenue, ...). */
    public abstract String name();

    /** Nome do parâmetro de consulta (date, month, ...). */
    public abstract String parameter();

    protected abstract P parse(String raw);

    protected abstract Table table(P period);

    public final String generate(String raw) {
        P period = parse(raw);
        Table table = table(period);
        List<String> lines = new ArrayList<>();
        lines.add(PROPERTY_NAME + "," + name() + "," + period);
        lines.add(table.header());
        lines.addAll(table.rows());
        lines.add(table.total());
        return String.join("\n", lines) + "\n";
    }
}
