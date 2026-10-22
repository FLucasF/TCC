package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    public static BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return round(a.add(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return BigDecimal.ZERO;
        return round(a.multiply(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return round(a.subtract(b));
    }

    public static BigDecimal percentage(BigDecimal value, int percent) {
        if (value == null) return BigDecimal.ZERO;
        BigDecimal percentDecimal = new BigDecimal(percent).divide(new BigDecimal(100), 10, RoundingMode.HALF_EVEN);
        return round(value.multiply(percentDecimal));
    }
}
