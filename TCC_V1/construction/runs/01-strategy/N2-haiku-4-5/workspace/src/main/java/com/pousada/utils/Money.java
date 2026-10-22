package com.pousada.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return round(a.divide(b, SCALE, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return round(a.subtract(b));
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return round(a.add(b));
    }

    public static BigDecimal percentage(BigDecimal base, BigDecimal percent) {
        return multiply(base, divide(percent, BigDecimal.valueOf(100)));
    }
}
