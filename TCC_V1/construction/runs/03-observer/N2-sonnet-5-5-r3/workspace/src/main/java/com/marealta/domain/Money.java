package com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Todo valor em dinheiro tem 2 casas, arredondado "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = of(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    /** 1234.56 */
    public static String plain(BigDecimal value) {
        return of(value).toPlainString();
    }
}
