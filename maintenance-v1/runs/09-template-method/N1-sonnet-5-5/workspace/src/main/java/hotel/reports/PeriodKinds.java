package hotel.reports;

import java.time.LocalDate;
import java.time.YearMonth;

/** The periods the reports are asked for. */
final class PeriodKinds {

    static final CsvReport.PeriodKind<LocalDate> DATE =
            new CsvReport.PeriodKind<>("date", "yyyy-MM-dd", LocalDate::parse);
    static final CsvReport.PeriodKind<YearMonth> MONTH =
            new CsvReport.PeriodKind<>("month", "yyyy-MM", YearMonth::parse);

    private PeriodKinds() {
    }
}
