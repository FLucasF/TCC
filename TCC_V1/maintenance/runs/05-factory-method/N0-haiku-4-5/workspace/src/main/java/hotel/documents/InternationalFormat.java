package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How money and dates are written in English documents. */
public final class InternationalFormat {

    private static final Locale ENGLISH = Locale.ENGLISH;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private InternationalFormat() {
    }

    public static String money(BigDecimal amount) {
        return "BRL " + String.format(ENGLISH, "%,.2f", amount);
    }

    public static String date(LocalDate date) {
        return ISO_DATE.format(date);
    }
}
