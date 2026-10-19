package br.com.marealta.reports;

import java.util.List;

/** Estrutura comum: título, cabeçalho, linhas e total. Cada relatório só diz o que muda. */
abstract class CsvReport<P> implements Report {
    static final String INN = "Pousada Maré Alta";

    record Table(List<String> header, List<List<String>> rows, List<String> total) {}

    private final String name;
    private final String paramName;

    CsvReport(String name, String paramName) {
        this.name = name;
        this.paramName = paramName;
    }

    @Override public String name() { return name; }

    @Override public String paramName() { return paramName; }

    abstract P parse(String value);

    abstract String label(P period);

    abstract Table table(P period);

    @Override
    public String render(String periodValue) {
        P period = parse(periodValue);
        Table table = table(period);
        StringBuilder csv = new StringBuilder();
        line(csv, List.of(INN, name, label(period)));
        line(csv, table.header());
        table.rows().forEach(row -> line(csv, row));
        line(csv, table.total());
        return csv.toString();
    }

    private static void line(StringBuilder csv, List<String> fields) {
        csv.append(String.join(",", fields)).append('\n');
    }
}
