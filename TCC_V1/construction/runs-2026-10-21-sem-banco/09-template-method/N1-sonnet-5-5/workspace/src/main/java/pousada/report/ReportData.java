package pousada.report;

import java.util.List;

public record ReportData(String period, List<String> header, List<List<String>> rows, List<String> total) {
}
