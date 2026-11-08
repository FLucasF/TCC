package pousada.report;

import java.time.YearMonth;
import java.util.regex.Pattern;

public abstract class MonthlyReport extends Report<YearMonth> {
    private static final Pattern FORMAT = Pattern.compile("\\d{4}-\\d{2}");

    protected MonthlyReport(String name) {
        super(name, "month");
    }

    @Override
    protected final YearMonth parse(String raw) {
        if (!FORMAT.matcher(raw).matches()) {
            throw new IllegalArgumentException(raw);
        }
        return YearMonth.parse(raw);
    }
}
