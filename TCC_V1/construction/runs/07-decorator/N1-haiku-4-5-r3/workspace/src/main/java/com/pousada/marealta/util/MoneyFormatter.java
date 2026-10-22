package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyFormatter {
    private static final DecimalFormat BRL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        BRL_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    public static String format(BigDecimal value) {
        return "R$ " + BRL_FORMAT.format(value);
    }
}
