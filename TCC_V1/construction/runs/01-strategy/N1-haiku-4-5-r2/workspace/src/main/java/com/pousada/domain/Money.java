package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final int SCALE = 2;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal multiply(BigDecimal a, double b) {
        return multiply(a, BigDecimal.valueOf(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return round(a.divide(b, SCALE + 2, ROUNDING));
    }

    public static BigDecimal percentOf(BigDecimal value, double percent) {
        return multiply(value, percent / 100.0);
    }
}
