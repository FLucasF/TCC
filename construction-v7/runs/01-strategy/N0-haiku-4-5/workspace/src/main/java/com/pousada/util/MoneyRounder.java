package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyRounder {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return round(a.divide(b, SCALE, ROUNDING_MODE));
    }

    public static BigDecimal percentage(BigDecimal value, int percent) {
        return multiply(value, BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE));
    }
}
