package hotel.reports;

import java.time.LocalDate;
import java.time.YearMonth;

/** Formats reports with title header. */
public final class ReportFormatter {

    private static final String HOTEL_NAME = "Pousada Maré Alta";

    private ReportFormatter() {}

    public static String withTitle(String reportName, LocalDate date, String csv) {
        String title = HOTEL_NAME + "," + reportName + "," + date;
        return title + "\n" + csv;
    }

    public static String withTitle(String reportName, YearMonth month, String csv) {
        String title = HOTEL_NAME + "," + reportName + "," + month;
        return title + "\n" + csv;
    }
}
