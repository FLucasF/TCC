package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {
    private static final DecimalFormat displayFormat;
    private static final DecimalFormat csvFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        displayFormat = new DecimalFormat("R$ #,##0.00", symbols);

        DecimalFormatSymbols csvSymbols = new DecimalFormatSymbols(Locale.US);
        csvFormat = new DecimalFormat("0.00", csvSymbols);
    }

    public static String format(BigDecimal value) {
        return displayFormat.format(value);
    }

    public static String formatForCsv(BigDecimal value) {
        return csvFormat.format(value);
    }
}
