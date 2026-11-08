package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal applyHighSeasonRate(BigDecimal baseRate) {
        return round(baseRate.multiply(BigDecimal.valueOf(1.20)));
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, int ratePlanPercent) {
        BigDecimal discountAmount = subtotal.multiply(BigDecimal.valueOf(ratePlanPercent)).divide(BigDecimal.valueOf(100), SCALE + 2, RoundingMode.HALF_EVEN);
        return round(discountAmount);
    }

    public static String formatMoney(BigDecimal value) {
        return formatMoneyPrecise(value);
    }

    public static String formatMoneyPrecise(BigDecimal value) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.of("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(value);
    }
}
