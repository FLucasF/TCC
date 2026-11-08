package com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final DecimalFormatSymbols BR = symbols();

    private Money() {}

    private static DecimalFormatSymbols symbols() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return s;
    }

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** Percentual (ex.: "0.12") aplicado a um valor, já arredondado. */
    public static BigDecimal percent(BigDecimal value, String rate) {
        return round(value.multiply(new BigDecimal(rate)));
    }

    /** R$ 1.234,56 */
    public static String format(BigDecimal v) {
        return "R$ " + new DecimalFormat("#,##0.00", BR).format(v);
    }
}
