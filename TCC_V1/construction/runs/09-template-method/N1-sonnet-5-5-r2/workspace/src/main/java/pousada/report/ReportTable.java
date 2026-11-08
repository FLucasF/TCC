package pousada.report;

import java.util.List;

/** O conteúdo de um relatório: cabeçalho, linhas e a linha de total. */
public record ReportTable(List<String> header, List<List<String>> rows, List<String> total) {
}
