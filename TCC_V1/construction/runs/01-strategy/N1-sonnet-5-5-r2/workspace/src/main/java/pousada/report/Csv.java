package pousada.report;

import java.util.List;

/** Estrutura comum dos relatórios: cabeçalho, linhas e uma linha final "total". */
final class Csv {
    private Csv() {
    }

    static String render(String header, List<String> rows, String total) {
        StringBuilder out = new StringBuilder(header).append('\n');
        rows.forEach(row -> out.append(row).append('\n'));
        return out.append("total,").append(total).append('\n').toString();
    }
}
