package br.com.marealta.report;

import br.com.marealta.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The one place that decides how every report looks as CSV: title line, header, rows, total.
 * Anything common to all reports (e.g. a trailing "generated on" line) goes here.
 */
public final class ReportRenderer {

    public static final String INN_NAME = "Pousada Maré Alta";
    public static final String CONTENT_TYPE = "text/csv; charset=utf-8";

    private ReportRenderer() {}

    public static String money(BigDecimal value) {
        return Money.plain(value);
    }

    public static String render(ReportTable table) {
        StringBuilder out = new StringBuilder();
        line(out, List.of(INN_NAME, table.name(), table.period()));
        line(out, table.header());
        table.rows().forEach(row -> line(out, row));
        line(out, table.total());
        return out.toString();
    }

    private static void line(StringBuilder out, List<String> cells) {
        out.append(cells.stream().map(ReportRenderer::cell).collect(Collectors.joining(","))).append('\n');
    }

    private static String cell(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
