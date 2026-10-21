package br.com.marealta.reservas.reports;

import java.util.List;

/**
 * Conteúdo de um relatório, independente do formato de saída. O título e
 * qualquer linha comum a todos os relatórios são montados pelo {@link CsvReportWriter}.
 */
public record ReportTable(String name, String period, List<String> header,
                          List<List<String>> rows, List<String> total) {
}
