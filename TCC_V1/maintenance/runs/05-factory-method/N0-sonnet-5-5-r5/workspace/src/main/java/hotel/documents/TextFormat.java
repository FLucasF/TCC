package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How money and dates are written in the texts the guest reads (Brazilian Portuguese, with the English variants for foreign guests). */
public final class TextFormat {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TextFormat() {
    }

    public static String money(BigDecimal amount) {
        return "R$ " + String.format(BRAZIL, "%,.2f", amount);
    }

    /** Money in English texts: BRL 1,234.56. */
    public static String englishMoney(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }

    /** Dates in English texts: 2027-03-10. */
    public static String englishDate(LocalDate date) {
        return date.toString();
    }

    public static String date(LocalDate date) {
        return BRAZILIAN_DATE.format(date);
    }
}
