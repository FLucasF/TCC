package pousada.report;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Único lugar que define o formato de todos os relatórios CSV. */
public class ReportWriter {
    private static final String INN_NAME = "Pousada Maré Alta";

    public String write(Report report, ReportData data) {
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(INN_NAME, report.name(), data.period()));
        lines.add(data.header());
        lines.addAll(data.rows());
        lines.add(data.total());
        return lines.stream().map(ReportWriter::line).collect(Collectors.joining("\n", "", "\n"));
    }

    private static String line(List<String> cells) {
        return cells.stream().map(ReportWriter::cell).collect(Collectors.joining(","));
    }

    private static String cell(String value) {
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        return needsQuotes ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
