package com.pousadamarealta.domain.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyFormatter {
    private static final DecimalFormat FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        FORMATTER = new DecimalFormat("R$ #,##0.00", symbols);
    }

    public static String format(BigDecimal amount) {
        return FORMATTER.format(amount);
    }
}
