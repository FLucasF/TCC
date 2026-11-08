package br.com.marealta.report;

import java.util.List;

/** Conteúdo de um relatório, ainda sem formato. O total é a última linha. */
public record ReportData(String name, String period, List<String> header, List<List<String>> rows,
                         List<String> total) {}
