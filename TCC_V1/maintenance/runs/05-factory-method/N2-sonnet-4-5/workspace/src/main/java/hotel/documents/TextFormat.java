package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How money and dates are written in the texts the guest reads. */
public final class TextFormat {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private TextFormat() {
    }

    /** Brazilian Portuguese: R$ 1.234,56 */
    public static String money(BigDecimal amount) {
        return "R$ " + String.format(BRAZIL, "%,.2f", amount);
    }

    /** Brazilian Portuguese: dd/MM/yyyy */
    public static String date(LocalDate date) {
        return BRAZILIAN_DATE.format(date);
    }

    /** English: BRL 1,234.56 */
    public static String moneyEnglish(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }

    /** ISO format: yyyy-MM-dd */
    public static String dateIso(LocalDate date) {
        return ISO_DATE.format(date);
    }
}
