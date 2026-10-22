package com.pousada.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Money {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static BigDecimal round(BigDecimal value) {
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
        BigDecimal percentValue = new BigDecimal(percent).divide(new BigDecimal(100), SCALE, ROUNDING_MODE);
        return multiply(value, percentValue);
    }
}
