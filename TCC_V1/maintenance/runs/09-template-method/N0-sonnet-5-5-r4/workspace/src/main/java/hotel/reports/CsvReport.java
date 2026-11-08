package hotel.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The shape shared by every CSV report: a title line (inn, report name, period), then the header, the rows
 * and the total. Anything that must change in all the reports at once belongs here.
 */
final class CsvReport {

    private static final String INN = "Pousada Maré Alta";

    private final List<String> lines = new ArrayList<>();

    CsvReport(String reportName, Object period, String header) {
        line(INN, reportName, period);
        lines.add(header);
    }

    CsvReport line(Object... cells) {
        lines.add(Arrays.stream(cells).map(String::valueOf).collect(Collectors.joining(",")));
        return this;
    }

    String build() {
        StringBuilder csv = new StringBuilder();
        for (String line : lines) {
            csv.append(line).append('\n');
        }
        return csv.toString();
    }
}
