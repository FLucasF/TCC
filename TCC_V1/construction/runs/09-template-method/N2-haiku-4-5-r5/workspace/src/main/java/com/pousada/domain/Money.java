package com.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return round(a.multiply(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return round(a.divide(b, RoundingMode.HALF_EVEN));
    }

    public static BigDecimal percent(BigDecimal value, int percent) {
        BigDecimal multiplier = new BigDecimal(percent).divide(new BigDecimal(100), 10, RoundingMode.HALF_EVEN);
        return round(value.multiply(multiplier));
    }
}
