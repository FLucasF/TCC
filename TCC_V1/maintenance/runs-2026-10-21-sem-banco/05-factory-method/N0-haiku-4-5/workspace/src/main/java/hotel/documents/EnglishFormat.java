package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How money and dates are written in English texts (for foreign guests). */
public final class EnglishFormat {

    private static final Locale ENGLISH = Locale.US;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private EnglishFormat() {
    }

    public static String money(BigDecimal amount) {
        return "BRL " + String.format(ENGLISH, "%,.2f", amount);
    }

    public static String date(LocalDate date) {
        return ISO_DATE.format(date);
    }
}
