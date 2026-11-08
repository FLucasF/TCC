package pousada.report;

import java.util.List;

/** O que é próprio de cada relatório: cabeçalho, linhas e a linha de total. */
public record ReportData(List<String> header, List<List<String>> rows, List<String> total) {
}
