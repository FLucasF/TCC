package br.com.mealta.report;

import java.util.ArrayList;
import java.util.List;

/** O formato de todos os relatórios: título, cabeçalho, linhas e total. */
public class CsvRenderer {
    private static final String HOTEL = "Pousada Maré Alta";

    public String render(Report report) {
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(HOTEL, report.name(), report.period()));
        lines.add(report.columns());
        lines.addAll(report.rows());
        lines.add(report.total());
        StringBuilder csv = new StringBuilder();
        for (List<String> line : lines) {
            csv.append(String.join(",", line)).append('\n');
        }
        return csv.toString();
    }
}
