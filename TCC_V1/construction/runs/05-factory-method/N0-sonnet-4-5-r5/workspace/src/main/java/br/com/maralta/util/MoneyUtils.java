package br.com.maralta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyUtils {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Locale EN_US = Locale.US;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String formatBRL(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(PT_BR);
        return formatter.format(value).replace(' ', ' ');
    }

    public static String formatBRLInternational(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(EN_US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "BRL " + formatter.format(value);
    }
}
