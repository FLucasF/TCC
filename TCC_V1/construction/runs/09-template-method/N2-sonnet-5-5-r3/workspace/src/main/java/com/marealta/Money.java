package com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Ex.: R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        return "R$ " + String.format(Locale.of("pt", "BR"), "%,.2f", value);
    }

    /** Ex.: 1234.56 */
    public static String plain(BigDecimal value) {
        return value.toPlainString();
    }
}
