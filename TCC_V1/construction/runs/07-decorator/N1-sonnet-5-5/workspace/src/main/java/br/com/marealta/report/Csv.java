package br.com.marealta.report;

import java.util.List;

/** O esqueleto comum dos relatórios: cabeçalho, linhas e uma linha "total" no fim. */
final class Csv {
    private Csv() {
    }

    static String render(List<String> header, List<List<String>> rows, String total) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", header)).append('\n');
        for (List<String> row : rows) {
            csv.append(String.join(",", row)).append('\n');
        }
        csv.append("total,").append(total).append('\n');
        return csv.toString();
    }
}
