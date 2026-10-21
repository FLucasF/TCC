package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.ReservationService;

import java.util.List;
import java.util.stream.Collectors;

/** Único lugar que define o jeito dos relatórios CSV: título, cabeçalho, linhas, total. */
public class CsvReportWriter {
    public String write(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        line(sb, List.of(ReservationService.INN_NAME, table.name(), table.period()));
        line(sb, table.header());
        table.rows().forEach(row -> line(sb, row));
        line(sb, table.total());
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> cells) {
        sb.append(cells.stream().map(CsvReportWriter::escape).collect(Collectors.joining(","))).append('\n');
    }

    private static String escape(String cell) {
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            return '"' + cell.replace("\"", "\"\"") + '"';
        }
        return cell;
    }
}
