package hotel.reports;

import java.time.YearMonth;

/** A report about the reservations that start in a month; served at /reports/{name}?month=yyyy-MM. */
public abstract class MonthlyReport extends CsvReport<YearMonth> {
}
