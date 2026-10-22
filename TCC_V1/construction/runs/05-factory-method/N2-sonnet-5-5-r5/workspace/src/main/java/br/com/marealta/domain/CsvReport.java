package br.com.marealta.domain;

import java.util.ArrayList;
import java.util.List;

/** Esqueleto dos relatórios: cabeçalho, linhas das reservas e a linha "total". */
public abstract class CsvReport {
    protected abstract String header();

    protected abstract List<String> rows();

    protected abstract String totalLine();

    public final String render() {
        List<String> lines = new ArrayList<>();
        lines.add(header());
        lines.addAll(rows());
        lines.add(totalLine());
        return String.join("\n", lines) + "\n";
    }
}
