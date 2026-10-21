package pousada;

import java.util.List;

/** Todo relatório CSV: cabeçalho, linhas das reservas e uma linha "total". */
public abstract class CsvReport {
    protected abstract List<String> columns();

    protected abstract List<List<String>> rows();

    protected abstract String total();

    public final String render() {
        StringBuilder csv = new StringBuilder(String.join(",", columns())).append('\n');
        for (List<String> row : rows()) {
            csv.append(String.join(",", row)).append('\n');
        }
        return csv.append("total,").append(total()).append('\n').toString();
    }
}
