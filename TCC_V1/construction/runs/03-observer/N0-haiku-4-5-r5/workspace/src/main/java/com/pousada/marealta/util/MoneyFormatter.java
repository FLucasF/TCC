package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormatter {
    private static final NumberFormat FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public static String format(BigDecimal value) {
        return FORMATTER.format(value);
    }
}
