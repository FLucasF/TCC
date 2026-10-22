package br.com.marealta.report;

import java.util.ArrayList;
import java.util.List;

/** Todo relatório CSV segue a mesma sequência: cabeçalho, linhas e linha final "total". */
public abstract class CsvReport {
    public final String render() {
        List<String> lines = new ArrayList<>();
        lines.add(header());
        lines.addAll(rows());
        lines.add(total());
        return String.join("\n", lines) + "\n";
    }

    protected abstract String header();

    protected abstract List<String> rows();

    protected abstract String total();
}
