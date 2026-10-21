package br.com.pousadamarealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Money rounding and pt-BR formatting helpers. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Money() {}

    /** Rounds to cents, half-even. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Formats as "R$ 1.234,56". */
    public static String brl(BigDecimal value) {
        String[] parts = round(value).toPlainString().split("\\.");
        String integer = parts[0];
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < integer.length(); i++) {
            if (i > 0 && (integer.length() - i) % 3 == 0 && integer.charAt(i - 1) != '-') {
                grouped.append('.');
            }
            grouped.append(integer.charAt(i));
        }
        return "R$ " + grouped + "," + parts[1];
    }

    public static String brDate(LocalDate date) {
        return BR_DATE.format(date);
    }
}
