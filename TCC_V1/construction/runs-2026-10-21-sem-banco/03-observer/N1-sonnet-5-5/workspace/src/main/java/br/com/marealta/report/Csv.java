package br.com.marealta.report;

import java.util.List;

/** Monta o CSV: cabeçalho, linhas e a linha final de total. */
final class Csv {
    private Csv() {
    }

    static String render(List<String> header, List<List<String>> rows, List<String> footer) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", header)).append('\n');
        rows.forEach(row -> csv.append(String.join(",", row)).append('\n'));
        csv.append(String.join(",", footer)).append('\n');
        return csv.toString();
    }
}
