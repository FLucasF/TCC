package hotel.reports;

import java.util.function.Function;

abstract class CsvReport {

    private static final String INN_NAME = "Pousada Maré Alta";

    protected String formatCsv(String reportName, String period, Function<StringBuilder, StringBuilder> bodyBuilder) {
        StringBuilder csv = new StringBuilder();
        csv.append(INN_NAME).append(',').append(reportName).append(',').append(period).append('\n');
        return bodyBuilder.apply(csv).toString();
    }
}
