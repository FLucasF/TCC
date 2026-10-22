package br.com.mealta.report;

import java.util.function.Function;

/**
 * Um relatório disponível: o nome, o parâmetro de consulta que ele recebe e como montá-lo.
 * O {@code builder} recebe o texto do parâmetro e devolve o relatório.
 */
public record ReportDefinition(String name, String param, Function<String, Report> builder) {
}
