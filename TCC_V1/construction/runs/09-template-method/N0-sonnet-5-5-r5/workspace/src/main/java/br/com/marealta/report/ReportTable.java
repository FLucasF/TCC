package br.com.marealta.report;

import java.util.List;

/** Conteúdo de um relatório, ainda sem formato: o CsvRenderer decide como sai. */
public record ReportTable(String name, String period, List<String> header, List<List<String>> rows,
                          List<String> total) {}
