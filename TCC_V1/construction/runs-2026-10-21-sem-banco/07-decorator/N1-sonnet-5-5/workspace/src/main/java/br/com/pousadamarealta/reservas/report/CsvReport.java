package br.com.pousadamarealta.reservas.report;

import java.util.List;

/** O formato comum dos relatórios: cabeçalho, linhas e uma linha "total" no fim. */
final class CsvReport {

    private CsvReport() {
    }

    static String render(String header, List<String> rows, String total) {
        StringBuilder csv = new StringBuilder(header).append('\n');
        rows.forEach(row -> csv.append(row).append('\n'));
        return csv.append("total,").append(total).append('\n').toString();
    }
}
