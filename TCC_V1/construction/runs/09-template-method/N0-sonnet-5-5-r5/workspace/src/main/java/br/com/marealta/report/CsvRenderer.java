package br.com.marealta.report;

import java.util.List;

/**
 * Único lugar que dá o formato aos relatórios: título, cabeçalho, linhas e total.
 * Qualquer mudança comum a todos (como uma linha final com a data de emissão) entra aqui.
 */
public final class CsvRenderer {
    public static final String PROPERTY_NAME = "Pousada Maré Alta";

    private CsvRenderer() {}

    public static String render(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        line(sb, List.of(PROPERTY_NAME, table.name(), table.period()));
        line(sb, table.header());
        table.rows().forEach(row -> line(sb, row));
        line(sb, table.total());
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields.get(i)));
        }
        sb.append('\n');
    }

    private static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return '"' + field.replace("\"", "\"\"") + '"';
        }
        return field;
    }
}
