package pousada.report;

import java.time.LocalDate;
import java.util.regex.Pattern;

public abstract class DailyReport extends Report<LocalDate> {
    private static final Pattern FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    protected DailyReport(String name) {
        super(name, "date");
    }

    @Override
    protected final LocalDate parse(String raw) {
        if (!FORMAT.matcher(raw).matches()) {
            throw new IllegalArgumentException(raw);
        }
        return LocalDate.parse(raw);
    }
}
