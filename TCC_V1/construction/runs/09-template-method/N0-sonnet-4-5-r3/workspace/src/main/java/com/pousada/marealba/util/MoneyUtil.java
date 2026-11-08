package com.pousada.marealba.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyUtil {
    private static final Locale BRAZIL = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(BRAZIL);

    public static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String formatBrazilian(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    public static String formatCsv(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN).toString();
    }
}
