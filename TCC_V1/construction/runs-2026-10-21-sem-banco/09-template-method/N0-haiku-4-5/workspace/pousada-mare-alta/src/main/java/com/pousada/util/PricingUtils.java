package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingUtils {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal multiply(BigDecimal value, int times) {
        BigDecimal result = value.multiply(BigDecimal.valueOf(times));
        return round(result);
    }

    public static BigDecimal percent(BigDecimal value, int percent) {
        BigDecimal multiplier = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING_MODE);
        BigDecimal result = value.multiply(multiplier);
        return round(result);
    }
}
