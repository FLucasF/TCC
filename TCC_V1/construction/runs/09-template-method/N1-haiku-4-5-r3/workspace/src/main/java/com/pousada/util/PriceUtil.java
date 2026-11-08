package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceUtil {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal applyPercentage(BigDecimal value, BigDecimal percentage) {
        return round(value.multiply(percentage));
    }

    public static BigDecimal applyHighSeasonMultiplier(BigDecimal baseRate) {
        return multiply(baseRate, new BigDecimal("1.20"));
    }
}
