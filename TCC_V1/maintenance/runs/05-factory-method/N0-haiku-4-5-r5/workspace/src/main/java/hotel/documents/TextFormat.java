package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How money and dates are written in the texts the guest reads (Brazilian Portuguese). */
public final class TextFormat {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final Locale US = Locale.US;
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INTERNATIONAL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextFormat() {
    }

    public static String money(BigDecimal amount) {
        return "R$ " + String.format(BRAZIL, "%,.2f", amount);
    }

    public static String date(LocalDate date) {
        return BRAZILIAN_DATE.format(date);
    }

    public static String internationalMoney(BigDecimal amount) {
        return "BRL " + String.format(US, "%,.2f", amount);
    }

    public static String internationalDate(LocalDate date) {
        return INTERNATIONAL_DATE.format(date);
    }
}
