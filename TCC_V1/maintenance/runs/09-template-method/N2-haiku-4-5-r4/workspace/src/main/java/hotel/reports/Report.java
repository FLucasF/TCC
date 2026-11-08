package hotel.reports;

import java.util.ArrayList;
import java.util.List;

abstract class Report {

    protected static final String POUSADA_NAME = "Pousada Maré Alta";

    public final String generate() {
        StringBuilder csv = new StringBuilder();
        csv.append(title()).append('\n');
        csv.append(header()).append('\n');
        List<String> lines = new ArrayList<>();
        data(lines);
        for (String line : lines) {
            csv.append(line).append('\n');
        }
        csv.append(total()).append('\n');
        return csv.toString();
    }

    protected abstract String title();
    protected abstract String header();
    protected abstract void data(List<String> lines);
    protected abstract String total();
}
