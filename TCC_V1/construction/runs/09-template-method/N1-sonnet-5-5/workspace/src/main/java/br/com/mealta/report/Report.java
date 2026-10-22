package br.com.mealta.report;

import java.util.List;

/** O conteúdo de um relatório. Como ele vira CSV é assunto do {@link CsvRenderer}. */
public interface Report {
    /** Nome usado no título: occupancy, revenue, ... */
    String name();

    /** Período usado no título: uma data ou um mês. */
    String period();

    List<String> columns();

    List<List<String>> rows();

    /** Linha final, começando por "total". */
    List<String> total();
}
