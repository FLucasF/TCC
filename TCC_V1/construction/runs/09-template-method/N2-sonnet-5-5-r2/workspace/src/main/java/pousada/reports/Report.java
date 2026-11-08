package pousada.reports;

import pousada.domain.AppException;

import java.util.ArrayList;
import java.util.List;

/**
 * Esqueleto de todo relatório CSV: título, cabeçalho, linhas e total.
 * Cada relatório só diz o que é dele; qualquer mudança no formato comum entra em {@link #render}.
 */
public abstract class Report<P> {
    public static final String HOUSE = "Pousada Maré Alta";

    public record Body(List<String> rows, String total) {
    }

    /** Nome no título e na URL. */
    public abstract String name();

    /** Parâmetro de consulta que traz o período. */
    public abstract String periodParam();

    protected abstract P parsePeriod(String raw);

    protected abstract String header();

    protected abstract Body body(P period);

    protected String periodText(P period) {
        return period.toString();
    }

    public final String render(String rawPeriod) {
        P period;
        try {
            period = parsePeriod(rawPeriod);
        } catch (java.time.format.DateTimeParseException e) {
            throw AppException.invalid("Período inválido em " + periodParam());
        }
        Body body = body(period);
        List<String> lines = new ArrayList<>();
        lines.add(String.join(",", HOUSE, name(), periodText(period)));
        lines.add(header());
        lines.addAll(body.rows());
        lines.add(body.total());
        return String.join("\n", lines) + "\n";
    }
}
