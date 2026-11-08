package hotel.reports;

import java.util.ArrayList;
import java.util.List;

abstract class CsvReport {

    private static final String INN_NAME = "Pousada Maré Alta";

    protected abstract String reportName();

    protected abstract String period();

    protected abstract String headerLine();

    protected abstract List<String> dataLines();

    protected abstract String totalLine();

    public final String generate() {
        List<String> lines = new ArrayList<>();
        lines.add(titleLine());
        lines.add(headerLine());
        lines.addAll(dataLines());
        lines.add(totalLine());
        return String.join("\n", lines) + "\n";
    }

    private String titleLine() {
        return INN_NAME + "," + reportName() + "," + period();
    }
}
