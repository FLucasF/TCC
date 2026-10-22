package com.pousada.marealta.util;

import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormatter {
    private static final NumberFormat PT_BR_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public static String formatForDisplay(double value) {
        return PT_BR_FORMAT.format(value);
    }

    public static String formatForCSV(double value) {
        return String.format("%.2f", value);
    }
}
