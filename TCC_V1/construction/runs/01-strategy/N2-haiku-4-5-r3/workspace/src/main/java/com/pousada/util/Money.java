package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class Money {
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static String format(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return nf.format(value);
    }

    public static String formatForJson(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
