package com.pousadamareaita.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MoneyUtils {
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static String formatBRL(BigDecimal value) {
        if (value == null) {
            return null;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("R$ #,##0.00", symbols);
        return df.format(value);
    }

    public static String formatBRLRaw(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return String.format("%.2f", value).replace(",", ".");
    }

    public static String formatBRL_EN(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return String.format("BRL %,d.%02d", value.longValue() / 100, value.longValue() % 100);
    }

    public static String formatBRL_EN_Proper(BigDecimal value) {
        if (value == null) {
            return null;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        DecimalFormat df = new DecimalFormat("BRL #,##0.00", symbols);
        return df.format(value);
    }
}
