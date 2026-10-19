package br.com.marealta.report;

import br.com.marealta.domain.Reservation;

import java.util.List;

/** Um relatório CSV. Um relatório novo é uma classe nova + uma linha em {@link ReportRegistry}. */
public interface Report {
    /** Nome usado na URL e no título (ex.: "occupancy"). */
    String name();

    /** Nome do parâmetro de consulta (ex.: "date", "month"). */
    String parameter();

    /** Gera o CSV; lança {@link InvalidPeriodException} se o período vier mal formatado. */
    String generate(String period, List<Reservation> reservations);
}
