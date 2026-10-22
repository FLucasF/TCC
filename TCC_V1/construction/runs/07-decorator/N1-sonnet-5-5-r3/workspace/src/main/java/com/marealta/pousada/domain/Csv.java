package com.marealta.pousada.domain;

import java.util.List;

public final class Csv {
    private Csv() {
    }

    /** Cabeçalho, linhas e a linha final de total, cada uma terminada em quebra de linha. */
    public static String render(List<String> header, List<List<String>> rows, List<String> total) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", header)).append('\n');
        rows.forEach(row -> csv.append(String.join(",", row)).append('\n'));
        csv.append(String.join(",", total)).append('\n');
        return csv.toString();
    }
}
