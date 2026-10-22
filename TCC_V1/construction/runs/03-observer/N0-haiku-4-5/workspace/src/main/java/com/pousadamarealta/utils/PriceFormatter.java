package com.pousadamarealta.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class PriceFormatter {
    private static final NumberFormat FORMATTER = NumberFormat.getInstance(new Locale("pt", "BR"));

    static {
        FORMATTER.setMinimumFractionDigits(2);
        FORMATTER.setMaximumFractionDigits(2);
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return "R$ " + FORMATTER.format(value);
    }

    public static String formatForCsv(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return String.format("%.2f", value).replace(",", ".");
    }
}
