package br.com.marealta.report;

import java.util.List;
import java.util.function.Function;

/** Monta o CSV de qualquer relatório: cabeçalho, uma linha por item e a linha "total" no fim. */
final class Csv {
    private Csv() {
    }

    static <T> String build(List<String> columns, List<T> items, Function<T, List<?>> row, String total) {
        StringBuilder csv = new StringBuilder(String.join(",", columns)).append('\n');
        for (T item : items) {
            csv.append(String.join(",", row.apply(item).stream().map(String::valueOf).toList())).append('\n');
        }
        return csv.append("total,").append(total).append('\n').toString();
    }
}
