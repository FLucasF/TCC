package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Formato dos comprovantes: R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        var format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(value);
    }

    /** Formato dos CSVs: 1234.56 */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }
}
