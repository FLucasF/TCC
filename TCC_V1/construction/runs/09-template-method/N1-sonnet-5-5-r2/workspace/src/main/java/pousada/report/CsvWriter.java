package pousada.report;

import pousada.domain.Establishment;

import java.util.ArrayList;
import java.util.List;

/** O formato comum a todos os relatórios: título, cabeçalho, linhas e total. */
public final class CsvWriter {
    private CsvWriter() {
    }

    public static String write(Report report, Period period, ReportTable table) {
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(Establishment.NAME, report.name(), period.label()));
        lines.add(table.header());
        lines.addAll(table.rows());
        lines.add(table.total());
        StringBuilder csv = new StringBuilder();
        for (List<String> line : lines) {
            csv.append(String.join(",", line.stream().map(CsvWriter::escape).toList())).append('\n');
        }
        return csv.toString();
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
