package br.com.mare.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    public static BigDecimal of(String value) { return round(new BigDecimal(value)); }

    public static BigDecimal round(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_EVEN); }

    /** Formato dos comprovantes: R$ 1.234,56 */
    public static String format(BigDecimal value) {
        String plain = round(value).toPlainString();
        int dot = plain.indexOf('.');
        String integer = plain.substring(0, dot);
        String cents = plain.substring(dot + 1);
        StringBuilder grouped = new StringBuilder();
        int digits = integer.length();
        for (int i = 0; i < digits; i++) {
            grouped.append(integer.charAt(i));
            int remaining = digits - i - 1;
            if (remaining > 0 && remaining % 3 == 0) grouped.append('.');
        }
        return "R$ " + grouped + "," + cents;
    }
}
