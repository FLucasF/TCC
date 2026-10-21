package br.com.marealta.reports;

import java.util.ArrayList;
import java.util.List;

/**
 * Esqueleto de todo relatório CSV: título, cabeçalho, linhas, total.
 * Cada relatório define só o que é dele; qualquer mudança comum a todos (como uma linha final
 * com a data de emissão) entra em {@link #render}.
 *
 * @param <P> o período pedido (uma noite, um mês)
 * @param <T> o item que vira uma linha
 */
public abstract class Report<P, T> {
    private static final String INN = "Pousada Maré Alta";

    public abstract String name();

    /** Nome do parâmetro de consulta que traz o período. */
    public abstract String paramName();

    protected abstract P parse(String raw);

    protected abstract List<String> columns();

    protected abstract List<T> select(P period);

    protected abstract List<String> cells(T item);

    protected abstract List<String> total(P period, List<T> items);

    public final String render(String rawPeriod) {
        P period = parse(rawPeriod);
        List<T> items = select(period);
        List<String> lines = new ArrayList<>();
        lines.add(String.join(",", INN, name(), period.toString()));
        lines.add(String.join(",", columns()));
        items.forEach(item -> lines.add(String.join(",", cells(item))));
        lines.add(String.join(",", total(period, items)));
        return String.join("\n", lines) + "\n";
    }
}
