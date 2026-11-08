package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal round(double value) {
        return round(BigDecimal.valueOf(value));
    }

    public static String format(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE).toString();
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal percent(BigDecimal value, BigDecimal percent) {
        return round(value.multiply(percent).divide(BigDecimal.valueOf(100), ROUNDING_MODE));
    }

    public static BigDecimal percent(BigDecimal value, double percent) {
        return percent(value, BigDecimal.valueOf(percent));
    }
}
