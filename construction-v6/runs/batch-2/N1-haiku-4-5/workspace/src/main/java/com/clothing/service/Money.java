package com.clothing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    public static BigDecimal round(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.setScale(2, RoundingMode.HALF_EVEN);
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
        return round(a.divide(b, 10, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
