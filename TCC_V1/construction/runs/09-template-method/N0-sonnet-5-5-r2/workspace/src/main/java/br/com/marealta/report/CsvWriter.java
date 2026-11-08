package br.com.marealta.report;

import br.com.marealta.service.ReservationService;
import java.util.ArrayList;
import java.util.List;

/** Único lugar que sabe como um relatório vira CSV: título, cabeçalho, linhas e total. */
public final class CsvWriter {
    public static final String CONTENT_TYPE = "text/csv; charset=utf-8";

    private CsvWriter() {}

    public static String render(ReportData report) {
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(ReservationService.HOTEL_NAME, report.name(), report.period()));
        lines.add(report.header());
        lines.addAll(report.rows());
        lines.add(report.total());
        StringBuilder sb = new StringBuilder();
        for (List<String> line : lines) {
            sb.append(String.join(",", line.stream().map(CsvWriter::escape).toList())).append('\n');
        }
        return sb.toString();
    }

    private static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }
}
