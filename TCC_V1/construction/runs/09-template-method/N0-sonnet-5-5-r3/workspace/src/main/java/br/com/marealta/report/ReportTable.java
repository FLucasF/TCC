package br.com.marealta.report;

import java.util.List;

/** The content of a report, independent of how it is written out. */
public record ReportTable(String name, String period, List<String> header, List<List<String>> rows,
                          List<String> total) {}
