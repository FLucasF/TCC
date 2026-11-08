package br.com.pousadamarealta.report;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ReportGenerator {
    private static final String POUSADA_NAME = "Pousada Maré Alta";

    public String generate(String period) {
        StringBuilder csv = new StringBuilder();

        // Título
        csv.append(POUSADA_NAME).append(",")
           .append(getReportName()).append(",")
           .append(period).append("\n");

        // Cabeçalho
        csv.append(String.join(",", getHeaders())).append("\n");

        // Linhas de dados
        List<String> dataLines = getDataLines();
        for (String line : dataLines) {
            csv.append(line).append("\n");
        }

        // Total
        csv.append(getTotalLine());

        return csv.toString();
    }

    protected abstract String getReportName();
    protected abstract List<String> getHeaders();
    protected abstract List<String> getDataLines();
    protected abstract String getTotalLine();
}
