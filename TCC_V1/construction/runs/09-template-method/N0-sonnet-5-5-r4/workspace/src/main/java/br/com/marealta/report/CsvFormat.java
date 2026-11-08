package br.com.marealta.report;

import br.com.marealta.domain.Pousada;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The one place that decides how every report looks as CSV: title line, header, rows and total.
 * A change to the layout of all reports (such as a closing line with the issue date) goes here.
 */
public final class CsvFormat {
    private CsvFormat() {
    }

    public static String render(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        line(sb, List.of(Pousada.NAME, table.name(), table.period()));
        line(sb, table.header());
        table.rows().forEach(row -> line(sb, row));
        line(sb, table.total());
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> cells) {
        sb.append(cells.stream().map(CsvFormat::escape).collect(Collectors.joining(","))).append('\n');
    }

    private static String escape(String cell) {
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
            return '"' + cell.replace("\"", "\"\"") + '"';
        }
        return cell;
    }
}
