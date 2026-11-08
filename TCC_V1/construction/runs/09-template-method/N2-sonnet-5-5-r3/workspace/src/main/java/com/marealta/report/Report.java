package com.marealta.report;

import java.util.List;

/**
 * Esqueleto de todo relatório CSV: título, cabeçalho, linhas e total.
 * Qualquer mudança no jeito dos relatórios (ex.: linha final com a data de emissão) é feita aqui, uma vez só.
 */
public abstract class Report {
    private static final String ESTABLISHMENT = "Pousada Maré Alta";

    public final String render() {
        StringBuilder out = new StringBuilder();
        line(out, List.of(ESTABLISHMENT, name(), period()));
        line(out, header());
        rows().forEach(row -> line(out, row));
        line(out, total());
        return out.toString();
    }

    private static void line(StringBuilder out, List<String> cells) {
        out.append(String.join(",", cells)).append('\n');
    }

    protected abstract String name();

    protected abstract String period();

    protected abstract List<String> header();

    protected abstract List<List<String>> rows();

    protected abstract List<String> total();
}
