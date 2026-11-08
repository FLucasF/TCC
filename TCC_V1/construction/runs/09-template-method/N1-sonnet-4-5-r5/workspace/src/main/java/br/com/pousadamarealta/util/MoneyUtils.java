package br.com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyUtils {
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "BR"));
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("R$ #,##0.00", SYMBOLS);
    private static final DecimalFormat CSV_FORMAT = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    public static String formatCsv(BigDecimal value) {
        return CSV_FORMAT.format(value);
    }
}
