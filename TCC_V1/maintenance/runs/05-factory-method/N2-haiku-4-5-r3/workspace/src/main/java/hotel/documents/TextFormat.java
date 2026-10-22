package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** How money and dates are written in the texts the guest reads. */
public final class TextFormat {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TextFormat() {
    }

    public static String moneyPtBr(BigDecimal amount) {
        return "R$ " + String.format(BRAZIL, "%,.2f", amount);
    }

    public static String money(BigDecimal amount) {
        return moneyPtBr(amount);
    }

    public static String datePtBr(LocalDate date) {
        return BRAZILIAN_DATE.format(date);
    }

    public static String date(LocalDate date) {
        return datePtBr(date);
    }

    public static String moneyEn(BigDecimal amount) {
        return "BRL " + String.format(Locale.US, "%,.2f", amount);
    }

    public static String dateEn(LocalDate date) {
        return ISO_DATE.format(date);
    }
}
