package br.com.marealta.pousada.service;

import java.util.List;

/** O conteúdo de um relatório: cabeçalho, linhas e a linha final de total. */
public record Report(List<String> columns, List<List<String>> rows, List<String> total) {

    public String toCsv() {
        StringBuilder csv = new StringBuilder(String.join(",", columns)).append('\n');
        rows.forEach(row -> csv.append(String.join(",", row)).append('\n'));
        csv.append(String.join(",", total)).append('\n');
        return csv.toString();
    }
}
