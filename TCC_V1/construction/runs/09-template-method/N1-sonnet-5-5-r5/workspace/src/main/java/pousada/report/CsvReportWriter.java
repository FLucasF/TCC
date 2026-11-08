package pousada.report;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pousada.domain.Pousada;

/** O jeito de todos os relatórios: título, cabeçalho, linhas e total. */
public class CsvReportWriter {

    public String write(Report report, ReportContent content) {
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(Pousada.NAME, report.name(), content.period()));
        lines.add(content.header());
        lines.addAll(content.rows());
        lines.add(content.total());
        // Linhas que devem terminar todos os relatórios entram aqui.
        return lines.stream().map(CsvReportWriter::line).collect(Collectors.joining("\n", "", "\n"));
    }

    private static String line(List<String> cells) {
        return cells.stream().map(CsvReportWriter::cell).collect(Collectors.joining(","));
    }

    private static String cell(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
