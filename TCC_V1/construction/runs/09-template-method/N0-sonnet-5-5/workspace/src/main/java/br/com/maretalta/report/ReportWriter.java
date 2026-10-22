package br.com.maretalta.report;

import java.util.List;
import java.util.stream.Collectors;

/** Único lugar que sabe como um relatório vira CSV. Mudou o jeito dos relatórios? Mude aqui. */
public final class ReportWriter {
    public static final String ESTABLISHMENT = "Pousada Maré Alta";

    private ReportWriter() {}

    public static String toCsv(Report report) {
        StringBuilder sb = new StringBuilder();
        line(sb, List.of(ESTABLISHMENT, report.name(), report.period()));
        line(sb, report.header());
        report.rows().forEach(row -> line(sb, row));
        line(sb, report.total());
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> fields) {
        sb.append(fields.stream().map(ReportWriter::escape).collect(Collectors.joining(","))).append('\n');
    }

    private static String escape(String f) {
        if (f.contains(",") || f.contains("\"") || f.contains("\n") || f.contains("\r")) {
            return '"' + f.replace("\"", "\"\"") + '"';
        }
        return f;
    }
}
