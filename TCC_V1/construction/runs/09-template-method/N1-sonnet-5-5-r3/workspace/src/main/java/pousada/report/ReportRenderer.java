package pousada.report;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Único lugar que define o jeito de todos os relatórios: título, cabeçalho, linhas e total.
 * Qualquer mudança no formato (por exemplo uma linha final com a data de emissão) entra aqui.
 */
public final class ReportRenderer {
    static final String PLACE = "Pousada Maré Alta";

    private ReportRenderer() {
    }

    public static String render(String reportName, String period, ReportData data) {
        StringBuilder csv = new StringBuilder();
        line(csv, List.of(PLACE, reportName, period));
        line(csv, data.header());
        data.rows().forEach(row -> line(csv, row));
        line(csv, data.total());
        return csv.toString();
    }

    private static void line(StringBuilder csv, List<String> fields) {
        csv.append(fields.stream().map(ReportRenderer::escape).collect(Collectors.joining(","))).append('\n');
    }

    private static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }
}
