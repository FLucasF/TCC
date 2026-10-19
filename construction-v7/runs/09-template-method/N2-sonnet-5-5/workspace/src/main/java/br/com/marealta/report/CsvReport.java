package br.com.marealta.report;

import br.com.marealta.domain.Reservation;

import java.util.ArrayList;
import java.util.List;

/** Esqueleto: título, cabeçalho, linhas e total. As subclasses só dizem o conteúdo. */
public abstract class CsvReport<P> implements Report {
    static final String POUSADA = "Pousada Maré Alta";

    /** Conteúdo de um relatório, sem formatação. */
    protected record Table(List<String> header, List<List<String>> rows, List<String> total) {}

    protected abstract P parse(String period);

    protected abstract Table build(P period, List<Reservation> reservations);

    @Override
    public final String generate(String period, List<Reservation> reservations) {
        if (period == null) throw new InvalidPeriodException("Parâmetro '" + parameter() + "' é obrigatório");
        P parsed = parse(period);
        Table table = build(parsed, reservations);
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(POUSADA, name(), period));
        lines.add(table.header());
        lines.addAll(table.rows());
        lines.add(table.total());
        StringBuilder sb = new StringBuilder();
        for (List<String> line : lines) sb.append(String.join(",", line)).append('\n');
        return sb.toString();
    }
}
