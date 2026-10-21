package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TextFormatEnglish {

    private static final Locale US = Locale.US;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private TextFormatEnglish() {
    }

    public static String money(BigDecimal amount) {
        return "BRL " + String.format(US, "%,.2f", amount);
    }

    public static String date(LocalDate date) {
        return ISO_DATE.format(date);
    }
}
